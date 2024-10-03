package mod.lcwalker.minishaderscore.shader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

import javax.vecmath.Matrix4f;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mod.lcwalker.minishaderscore.ModMain;
import mod.lcwalker.minishaderscore.util.ResourceManager;
import mod.lcwalker.minishaderscore.util.UseShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ReShaderGroup {
	
	private static final Minecraft mc = Minecraft.getMinecraft();
	private final IResourceManager manager;
	private final List<ReShader> shadersList = Lists.newArrayList();
	private final Map<String,Framebuffer> mframebuffers = Maps.newHashMap();
	private final List<Framebuffer> lframebuffers = Lists.newArrayList();
	private final Framebuffer frame;
	private static Matrix4f projectionMatrix;
	private static int mainFramebufferWidth;
    private static int mainFramebufferHeight;
    private static float time;
    private static float lastStamp;
	
	public static final String path = "./" + ModMain.NAME + "/";
	public static final File shaderspostFile = new File(path + ResourceManager.shaders_post);
	public static final File shadersprogramFile = new File(path + ResourceManager.shaders_program);
	
	public static List<File> resFiles = new ArrayList<>();
	public static File jsonf = null;
	private static String jsonName;
	
	public ReShaderGroup(TextureManager textureManager, IResourceManager resourceManager, Framebuffer framebuffer, String jsonName) {
		
		jsonf = new File(shaderspostFile, jsonName + ".json");
		
		ReShaderGroup.jsonName = jsonName;
		
		this.manager = resourceManager;
        this.frame = framebuffer;
        time = 0.0F;
        lastStamp = 0.0F;
        mainFramebufferWidth = framebuffer.framebufferWidth;
        mainFramebufferHeight = framebuffer.framebufferHeight;
        this.resetProjectionMatrix();
		this.readJson(textureManager);
	}
	
	public void readJson(TextureManager textureManager) {
		
		try {
			this.readJson(textureManager, jsonf);
		}
		catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void addList() {
		
		if (shaderspostFile.exists()) {
			
			File[] listFiles = shaderspostFile.listFiles();
            for (File file : Objects.requireNonNull(listFiles)) {

                if (!file.isDirectory() && file.getName().endsWith(".json")) {
                    resFiles.add(file);
                }
            }
		}
		else {
			shaderspostFile.mkdirs();
		}
		
		if (!shadersprogramFile.exists()){
			shadersprogramFile.mkdirs();
		}
	}
	
	private void readJson(TextureManager textureManager, File jsonFile) {
		
		JsonParser jsonparser = new JsonParser();
		InputStream is = null;
		
		try {
					
			if (!resFiles.isEmpty() && resFiles.contains(jsonFile)) {
				
				UseShader.theShaderGroup = null;
				
				is = Files.newInputStream(jsonFile.toPath());
    			JsonObject jsonobject = jsonparser.parse(IOUtils.toString(is, Charsets.UTF_8)).getAsJsonObject();
                JsonArray jsonarray;

                Iterator<JsonElement> iterator;
                JsonElement jsonelement;
                
                if (JsonUtils.isJsonArray(jsonobject, "targets")) {
                	
                    jsonarray = jsonobject.getAsJsonArray("targets");

                    for (iterator = jsonarray.iterator(); iterator.hasNext(); ) {
                    	
                        jsonelement = iterator.next();
                        try {
                        	this.initTarget(jsonelement);
                        }
                        catch (Exception e) {
                        	e.printStackTrace();
                        }
                    }
                }

                if (JsonUtils.isJsonArray(jsonobject, "passes")) {
                	
                    jsonarray = jsonobject.getAsJsonArray("passes");

                    for (iterator = jsonarray.iterator(); iterator.hasNext(); ) {
                    	
                        jsonelement = iterator.next();
                        try {
                        	this.parsePass(textureManager, jsonelement);
                        }
                        catch (Exception e1) {
                        	e1.printStackTrace();
                        }
                    }
                }
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	private void initTarget(JsonElement jsonElement) {
		
        if (JsonUtils.isString(jsonElement)) {
        	addFramebuffer(jsonElement.getAsString(), mainFramebufferWidth, mainFramebufferHeight);
        }
        else {
        	
            JsonObject jsonobject = JsonUtils.getJsonObject(jsonElement, "target");
            String s = JsonUtils.getString(jsonobject, "name");
            
            int width = JsonUtils.getInt(jsonobject, "width", mainFramebufferWidth);
            int height = JsonUtils.getInt(jsonobject, "height", mainFramebufferHeight);

            addFramebuffer(s, width, height);
        }
    }
	
	private void parsePass(TextureManager textureManager, JsonElement jsonElement) {
		
        JsonObject jsonobject = JsonUtils.getJsonObject(jsonElement, "pass");
        
        String s = JsonUtils.getString(jsonobject, "name");
        String s1 = JsonUtils.getString(jsonobject, "intarget");
        String s2 = JsonUtils.getString(jsonobject, "outtarget");
        
        Framebuffer framebuffer = this.getFramebuffer(s1);
        Framebuffer framebuffer1 = this.getFramebuffer(s2);

        if (!(framebuffer == null && framebuffer1 == null)){
        	
            ReShader shader = this.addShader(s, framebuffer, framebuffer1);
            
            JsonArray jsonarray = JsonUtils.getJsonArray(jsonobject, "auxtargets", null);
            for (JsonElement jsonelement1 : jsonarray) {

                try {

                    JsonObject jsonobject1 = JsonUtils.getJsonObject(jsonelement1, "auxtarget");

                    String s4 = JsonUtils.getString(jsonobject1, "name");
                    String s3 = JsonUtils.getString(jsonobject1, "id");

                    Framebuffer framebuffer2 = this.getFramebuffer(s3);
                    if (framebuffer2 == null) {

                        ResourceLocation resourcelocation = new ResourceLocation(ModMain.MODID + ":" + "textures/effect/" + s3 + ".png");
                        try {
                            this.manager.getResource(resourcelocation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        textureManager.bindTexture(resourcelocation);
                        ITextureObject itextureobject = textureManager.getTexture(resourcelocation);

                        int width = JsonUtils.getInt(jsonobject1, "width");
                        int height = JsonUtils.getInt(jsonobject1, "height");
                        boolean flag = JsonUtils.getBoolean(jsonobject1, "bilinear");

                        if (flag) {

                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                        } else {

                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                        }
                        shader.addAuxFramebuffer(s4, itextureobject.getGlTextureId(), width, height);
                    } else {
                        shader.addAuxFramebuffer(s4, framebuffer2, framebuffer2.framebufferTextureWidth, framebuffer2.framebufferTextureHeight);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            JsonArray jsonarray1 = JsonUtils.getJsonArray(jsonobject, "uniforms", null);
            for (JsonElement jsonelement2 : jsonarray1) {

                try {
                    this.initUniform(jsonelement2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initUniform(JsonElement jsonElement) {
    	
        JsonObject jsonobject = JsonUtils.getJsonObject(jsonElement, "uniform");
        String s = JsonUtils.getString(jsonobject, "name");
        
        ReShaderUniform shaderuniform = this.shadersList.get(this.shadersList.size() - 1).getShaderManager().getShaderUniform(s);
        if (shaderuniform != null) {
        	
        	float[] afloat = new float[4];
            int i = 0;
            
            JsonArray jsonarray = JsonUtils.getJsonArray(jsonobject, "values");
            for (Iterator<JsonElement> iterator = jsonarray.iterator(); iterator.hasNext(); ++i) {
            	
                JsonElement jsonelement1 = iterator.next();
                try {
                    afloat[i] = JsonUtils.getFloat(jsonelement1, "value");
                }
                catch (Exception e) {
                	e.printStackTrace();
                }
            }

            switch (i) {
                case 0:
                    break;
                case 1:
                    shaderuniform.floatBufferPut(afloat[0]);
                    break;
                case 2:
                    shaderuniform.floatBufferPut(afloat[0], afloat[1]);
                    break;
                case 3:
                    shaderuniform.floatBufferPut(afloat[0], afloat[1], afloat[2]);
                    break;
                case 4:
                    shaderuniform.floatBufferPut(afloat[0], afloat[1], afloat[2], afloat[3]);
            }
        }
    }

    public void addFramebuffer(String s, int width, int height) {
    	
        Framebuffer framebuffer = new Framebuffer(width, height, true);
        framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        
        this.mframebuffers.put(s, framebuffer);

        if (width == mainFramebufferWidth && height == mainFramebufferHeight) {
            this.lframebuffers.add(framebuffer);
        }
    }
    
    public void deleteShaderGroup() {

        for (Framebuffer framebuffer : this.mframebuffers.values()) {

            framebuffer.deleteFramebuffer();
        }

        for (ReShader shader : this.shadersList) {

            shader.deleteShader();
        }

        this.shadersList.clear();
    }
    
    public ReShader addShader(String name, Framebuffer framebuffer, Framebuffer framebuffer1) {
		
		ReShader shader = null;
		
		try {
			shader = new ReShader(this.manager, name, framebuffer, framebuffer1);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		this.shadersList.add(this.shadersList.size(), shader);
		
		return shader;
	}
    
    private void resetProjectionMatrix() {
    	
        projectionMatrix = new Matrix4f();
        projectionMatrix.setIdentity();
        projectionMatrix.m00 = 2.0F / (float) this.frame.framebufferTextureWidth;
        projectionMatrix.m11 = 2.0F / (float) (-this.frame.framebufferTextureHeight);
        projectionMatrix.m22 = -0.0020001999F;
        projectionMatrix.m33 = 1.0F;
        projectionMatrix.m03 = -1.0F;
        projectionMatrix.m13 = 1.0F;
        projectionMatrix.m23 = -1.0001999F;
    }
    
    public void createBindFramebuffers(int width, int height) {
    	
        mainFramebufferWidth = this.frame.framebufferTextureWidth;
        mainFramebufferHeight = this.frame.framebufferTextureHeight;
        
        this.resetProjectionMatrix();

        for (ReShader shader : this.shadersList) {

            shader.setProjectionMatrix(projectionMatrix);
        }

        for (Framebuffer framebuffer : this.lframebuffers) {

            framebuffer.createBindFramebuffer(width, height);
        }
    }

    public void loadShaderGroup(float f) {
    	
        if (f < lastStamp) {
        	
            time += 1.0F - lastStamp;
            time += f;
        }
        else {
            time += f - lastStamp;
        }

        for (lastStamp = f; time > 20.0F; time -= 20.0F);

        for (ReShader shader : this.shadersList) {

            try {
                shader.loadShader(time / 20.0F);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getName() {
    	return jsonName;
    }
    
    private Framebuffer getFramebuffer(String s) {
        return s == null ? null : (s.equals("minecraft:main") ? this.frame : this.mframebuffers.get(s));
    }
}
