package li.gerard.crystallomachia;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;

import li.gerard.crystallomachia.golem.GolemData;

@LDLibPlugin
public class CrystallomachiaLDLibPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        AccessorRegistries.registerAccessor(GolemData.ACCESSOR, 1000);
    }
}
