package mod.lcwalker.minishaderscore.shader;

import java.util.List;

import javax.vecmath.Matrix4f;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ReShader {
	
	private static final Minecraft mc = Minecraft.getMinecraft();
    private final ReShaderManager manager;
    public final Framebuffer framebufferIn;
    public final Framebuffer framebufferOut;
    private final List<Object> listAuxFramebuffers = Lists.newArrayList();
    private final List<String> listAuxNames = Lists.newArrayList();
    private final List<Integer> listAuxWidths = Lists.newArrayList();
    private final List<Integer> listAuxHeights = Lists.newArrayList();
    private static Matrix4f projectionMatrix;

    public ReShader(IResourceManager iResourceManager, String name, Framebuffer framebuffer1, Framebuffer framebuffer2) throws Exception {
    	
    	this.manager = new ReShaderManager(iResourceManager, name);
    	
    	this.framebufferIn = framebuffer1;
    	this.framebufferOut = framebuffer2;
    }

    public void deleteShader() {
        this.manager.deleteShader();
    }

    public void addAuxFramebuffer(String name, Object object, int width, int height) {
    	
        this.listAuxNames.add(this.listAuxNames.size(), name);
        this.listAuxFramebuffers.add(this.listAuxFramebuffers.size(), object);
        this.listAuxWidths.add(this.listAuxWidths.size(), width);
        this.listAuxHeights.add(this.listAuxHeights.size(), height);
    }

    public void preLoadShader() {
    	
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_COLOR_MATERIAL);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void setProjectionMatrix(Matrix4f matrix4f) {
        projectionMatrix = matrix4f;
    }

    public void loadShader(float time) {
    	
        preLoadShader();
        
        framebufferIn.unbindFramebuffer();
        float f = (float) framebufferOut.framebufferTextureWidth;
        float f1 = (float) framebufferOut.framebufferTextureHeight;
        
        GL11.glViewport(0, 0, (int) f, (int) f1);
        
        manager.addSamplerTexture("DiffuseSampler", framebufferIn);
        
        for (int i = 0; i < listAuxFramebuffers.size(); ++i) {
        	
            manager.addSamplerTexture(listAuxNames.get(i), listAuxFramebuffers.get(i));
            manager.getShaderUniformOrDefault("AuxSize" + i).floatBufferPut((float) listAuxWidths.get(i), (float) listAuxHeights.get(i));
        }

        manager.getShaderUniformOrDefault("ProjMat").matrix(projectionMatrix);
        manager.getShaderUniformOrDefault("InSize").floatBufferPut((float) framebufferIn.framebufferTextureWidth, (float) framebufferIn.framebufferTextureHeight);
        manager.getShaderUniformOrDefault("OutSize").floatBufferPut(f, f1);
        manager.getShaderUniformOrDefault("Time").floatBufferPut(time);
        manager.getShaderUniformOrDefault("ScreenSize").floatBufferPut((float) mc.displayWidth, (float) mc.displayHeight);
        
        manager.useShader();
        
        framebufferOut.framebufferClear();
        framebufferOut.bindFramebuffer(false);
        
        GL11.glDepthMask(false);
        GL11.glColorMask(true, true, true, false);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(0.0D, f, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferBuilder.pos(f, f, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferBuilder.pos(f, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        bufferBuilder.pos(0.0D, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
        
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        
        manager.endShader();
        
        framebufferOut.unbindFramebuffer();
        framebufferIn.unbindFramebufferTexture();

        for (Object object : listAuxFramebuffers) {

            if (object instanceof Framebuffer) {
                ((Framebuffer) object).unbindFramebufferTexture();
            }
        }
    }

    public ReShaderManager getShaderManager() {
        return this.manager;
    }
}