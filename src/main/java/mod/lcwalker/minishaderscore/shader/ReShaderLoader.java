package mod.lcwalker.minishaderscore.shader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;

import com.google.common.collect.Maps;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ReShaderLoader {
	
    private final ShaderType stype;
    private final String type;
    private final int shader;
    private int i = 0;

    private ReShaderLoader(ShaderType stype, int shader, String type) {
    	
        this.stype = stype;
        this.shader = shader;
        this.type = type;
    }

    public void attach(ReShaderManager manager) {
    	
        ++this.i;
        
        OpenGlHelper.glAttachShader(manager.getProgramObj(), this.shader);
    }

    public void delete(ReShaderManager manager) {
    	
        --this.i;
        if (this.i <= 0) {
        	
            OpenGlHelper.glDeleteShader(this.shader);
            
            this.stype.getLoadedShaders().remove(this.type);
        }
    }

    public static ReShaderLoader readFile(IResourceManager iResourceManager, ShaderType type, String name) throws Exception {
    	
        ReShaderLoader shaderloader = type.getLoadedShaders().get(name);

        if (shaderloader == null) {
        	
        	InputStream is = Files.newInputStream(new File(ReShaderGroup.shadersprogramFile.getCanonicalPath(), name + type.getShaderExtension()).toPath());
            BufferedInputStream bis = new BufferedInputStream(is);
            
            byte[] bytes = IOUtils.toByteArray(bis);
            
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.position(0);
            
            int id = OpenGlHelper.glCreateShader(type.getShaderMode());
            OpenGlHelper.glShaderSource(id, buffer);
            OpenGlHelper.glCompileShader(id);

            OpenGlHelper.glGetShaderi(id, OpenGlHelper.GL_COMPILE_STATUS);

            shaderloader = new ReShaderLoader(type, id, name);
            type.getLoadedShaders().put(name, shaderloader);
        }

        return shaderloader;
    }

    @SideOnly(Side.CLIENT)
    public enum ShaderType {
    	
    	VERTEX("vertex", ".vsh", OpenGlHelper.GL_VERTEX_SHADER),
        FRAGMENT("fragment", ".fsh", OpenGlHelper.GL_FRAGMENT_SHADER);
    	
        private final String name;
        private final String extension;
        private final int mode;
        private final Map<String,ReShaderLoader> map = Maps.newHashMap();

        ShaderType(String name, String extension, int mode) {
        	
            this.name = name;
            this.extension = extension;
            this.mode = mode;
        }

        public String getShaderName() {
            return this.name;
        }

        private String getShaderExtension() {
            return this.extension;
        }

        private int getShaderMode() {
            return this.mode;
        }

        private Map<String,ReShaderLoader> getLoadedShaders() {
            return this.map;
        }
    }
}