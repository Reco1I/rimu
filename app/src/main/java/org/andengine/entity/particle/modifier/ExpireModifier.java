package org.andengine.entity.particle.modifier;

import static org.andengine.util.math.MathUtils.RANDOM;

import org.andengine.entity.particle.Particle;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 21:21:10 - 14.03.2010
 */
public class ExpireModifier implements IParticleModifier {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private float mMinLifeTime;
	private float mMaxLifeTime;

	// ===========================================================
	// Constructors
	// ===========================================================

	public ExpireModifier(final float pLifeTime) {
		this(pLifeTime, pLifeTime);
	}

	public ExpireModifier(final float pMinLifeTime, final float pMaxLifeTime) {
		this.mMinLifeTime = pMinLifeTime;
		this.mMaxLifeTime = pMaxLifeTime;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public float getMinLifeTime() {
		return this.mMinLifeTime;
	}

	public float getMaxLifeTime() {
		return this.mMaxLifeTime;
	}

	public void setLifeTime(final float pLifeTime) {
		this.mMinLifeTime = pLifeTime;
		this.mMaxLifeTime = pLifeTime;
	}

	public void setLifeTime(final float pMinLifeTime, final float pMaxLifeTime) {
		this.mMinLifeTime = pMinLifeTime;
		this.mMaxLifeTime = pMaxLifeTime;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onInitializeParticle(final Particle pParticle) {
		pParticle.setExpireTime(RANDOM.nextFloat() * (this.mMaxLifeTime - this.mMinLifeTime) + this.mMinLifeTime);
	}

	@Override
	public void onUpdateParticle(final Particle pParticle) {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
