// BEGIN rimu-changed: Empty texture region with empty texture inside, used to handle nullability
// in Sprites.
package org.andengine.opengl.texture.region;

import org.andengine.opengl.texture.BlankTexture;

public class BlankTextureRegion extends TextureRegion {

    public BlankTextureRegion() {
        super(new BlankTexture(), 0, 0, 0, 0);
    }
}
// END rimu-changed.
