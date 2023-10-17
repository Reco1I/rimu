package game.rimu.ui.views.preference

import game.rimu.android.RimuContext
import game.rimu.constants.RimuSetting
import game.rimu.management.Setting
import game.rimu.ui.views.ConstraintLayout


sealed class SettingView<T : Any>(ctx: RimuContext, key: RimuSetting) : ConstraintLayout(ctx, {})
{

    protected val binding by Setting<T>(key)

}


class CheckBoxSetting(ctx: RimuContext, key: RimuSetting) : SettingView<Boolean>(ctx, key)
{



}