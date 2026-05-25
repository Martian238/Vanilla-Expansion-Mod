//作者：CN方柠檬FNM
//分端参考及扩展：DeepSeek

var isDesktop = !Vars.mobile;

// 通用反射写入函数
function setStaticFinalField(field, value) {
    if (isDesktop) {
        const Unsafe = java.lang.Class.forName("sun.misc.Unsafe");
        const unsafeField = Unsafe.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        const unsafe = unsafeField.get(null);
        const base = unsafe.staticFieldBase(field);
        const offset = unsafe.staticFieldOffset(field);
        unsafe.putObject(base, offset, value);
    } else {
        field.set(null, value);
    }
}

// 扩展阵营数组
const teamClass = Team.all[0].getClass();
const allField = teamClass.getDeclaredField("all");
allField.setAccessible(true);

const currentAll = Team.all;
if (currentAll.length < 9) {
    const newAll = java.lang.reflect.Array.newInstance(teamClass, 9);
    for (let i = 0; i < currentAll.length; i++) newAll[i] = currentAll[i];
    for (let i = currentAll.length; i < 9; i++) newAll[i] = new Team(i, "team-" + i);
    setStaticFinalField(allField, newAll);
}

// 使用索引 8（第 9 个阵营）
const myTeam = Team.all[8];
myTeam.name = "lacuna";
myTeam.color.set(Color.valueOf("00ffce"));
myTeam.setPalette(
    Color.valueOf("00ffce"),
    Color.valueOf("3ad2b5"),
    Color.valueOf("189885")
);
if (!isDesktop) myTeam.ignoreUnitCap = true;

// 扩展 baseTeams
const baseField = teamClass.getDeclaredField("baseTeams");
baseField.setAccessible(true);
const newBaseArr = java.lang.reflect.Array.newInstance(teamClass, 7);
for (let i = 0; i < 6; i++) newBaseArr[i] = Team.baseTeams[i];
newBaseArr[6] = myTeam;
setStaticFinalField(baseField, newBaseArr);

// 注册图标
Events.on(ClientLoadEvent, e => {
    const region = Core.atlas.find("ve-lacuna");
    if (region && region.found()) {
        Fonts.registerIcon("lacuna", "ve-lacuna", 0xE100, region);
        myTeam.emoji = Fonts.getUnicodeStr("lacuna");
    }
});