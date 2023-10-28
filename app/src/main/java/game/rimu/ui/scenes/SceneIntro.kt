package game.rimu.ui.scenes

import game.rimu.MainContext
import game.rimu.ui.entity.hitobjects.HitCircleEntity

class SceneIntro(ctx: MainContext) : BaseScene(ctx)
{

    val hitCircleTest = HitCircleEntity(ctx)

    init
    {
        attachChild(hitCircleTest)
        hitCircleTest.setPosition(ctx.engine.surface.width / 2f, ctx.engine.surface.height / 2f)
    }
}