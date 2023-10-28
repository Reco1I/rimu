// BEGIN rimu-changed: Despite 'EmptyTexture' this one is truly empty and no TextureManager instance
// needs to be passed to the constructor.
package org.andengine.opengl.texture;

import org.andengine.opengl.util.GLState;

import java.io.IOException;

public class BlankTexture implements ITexture {

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getHardwareTextureID() {
        return Texture.HARDWARE_TEXTURE_ID_INVALID;
    }

    @Override
    public boolean isLoadedToHardware() {
        return false;
    }

    @Override
    public void setNotLoadedToHardware() {
    }

    @Override
    public boolean isUpdateOnHardwareNeeded() {
        return false;
    }

    @Override
    public void setUpdateOnHardwareNeeded(boolean pUpdateOnHardwareNeeded) {
    }

    @Override
    public void load() {
    }

    @Override
    public void load(GLState pGLState) throws IOException {
    }

    @Override
    public void unload() {
    }

    @Override
    public void unload(GLState pGLState) {
    }

    @Override
    public void loadToHardware(GLState pGLState) throws IOException {
    }

    @Override
    public void unloadFromHardware(GLState pGLState) {
    }

    @Override
    public void reloadToHardware(GLState pGLState) throws IOException {
    }

    @Override
    public void bind(GLState pGLState) {
    }

    @Override
    public void bind(GLState pGLState, int pGLActiveTexture) {
    }

    @Override
    public PixelFormat getPixelFormat() {
        return PixelFormat.UNDEFINED;
    }

    @Override
    public TextureOptions getTextureOptions() {
        return TextureOptions.DEFAULT;
    }

    @Override
    public int getTextureMemorySize() {
        return 0;
    }

    @Override
    public boolean hasTextureStateListener() {
        return false;
    }

    @Override
    public ITextureStateListener getTextureStateListener() {
        return null;
    }

    @Override
    public void setTextureStateListener(ITextureStateListener pTextureStateListener) {
    }
}
// END rimu-changed.
