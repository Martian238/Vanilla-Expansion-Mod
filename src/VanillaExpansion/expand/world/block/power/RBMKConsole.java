package VanillaExpansion.expand.world.block.power;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Block;

public class RBMKConsole extends Block {
    public static final int GRID = 16;
    public static final int CELLS = 256;
    public static final int STEP = 2;
    public static final int ACT_TOGGLE_BASE = 1000;
    public static final int ACT_SELECT_ALL = 2000;
    public static final int ACT_SELECT_CLEAR = 2001;
    public static final int ACT_SELECT_ALL_RODS = 2002;
    public static final int ACT_GROUP_BASE = 3000;
    public static final int ACT_LEVEL_BASE = 4000;
    public static final int ACT_TIER_BASE = 5000;
    public static final int ACT_AZ5 = 6000;
    private static final String[] GROUP_NAMES = new String[]{"Red", "Yellow", "Green", "Blue", "Purple"};
    private static final Color[] GROUP_COLORS = new Color[]{Color.red, Color.yellow, Color.green, Color.blue, Color.purple};

    public RBMKConsole(String name) {
        super(name);
        this.size = 6;
        this.update = true;
        this.sync = true;
        this.solid = true;
        this.destructible = true;
        this.configurable = true;
        this.config(Integer.class, (tile, value) -> ((RBMKConsoleBuild)tile).handleCode((int)value));
        this.config(Point2.class, (tile, p) -> ((RBMKConsoleBuild)tile).setCenter(p.x, p.y));
        this.buildType = () -> new RBMKConsoleBuild();
    }

    public void setBars() {
        super.setBars();
        this.addBar("flux", e -> new Bar(() -> "Neutron Flux: " + (int)((RBMKConsoleBuild)e).fluxOut(), () -> Pal.reactorPurple, () -> Mathf.clamp((float)(((RBMKConsoleBuild)e).fluxOut() / 6000.0f))));
        this.addBar("status", e -> new Bar(() -> "Status: " + ((RBMKConsoleBuild)e).statusText(), () -> ((RBMKConsoleBuild)e).statusColor(), () -> Mathf.clamp((float)(((RBMKConsoleBuild)e).avgColumnHeat() / 1000.0f))));
    }

    public static Color typeColor(int type, float heat, float maxHeat) {
        if (type < 0 || type >= RBMKBase.ColumnType.values().length) {
            return Pal.darkMetal;
        }
        RBMKBase.ColumnType t = RBMKBase.ColumnType.values()[type];
        switch (t) {
            case FUEL:
            case FUEL_SIM:
            case BREEDER:
            case STORAGE:
            case BURNER: {
                return Color.yellow;
            }
            case CONTROL:
            case CONTROL_AUTO: {
                return Color.green;
            }
            case BOILER: {
                return Color.sky;
            }
            case MODERATOR: {
                return Color.blue;
            }
            case ABSORBER:
            case OUTGASSER: {
                return Color.gray;
            }
            case REFLECTOR: {
                return Color.purple;
            }
            case COOLER: {
                return Color.cyan;
            }
            case HEATEX: {
                return Color.gold;
            }
        }
        return Pal.darkMetal;
    }

    private static int parseInt(String text, int def) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clampInt(int value, int max) {
        return Mathf.clamp((int)value, (int)0, (int)max);
    }

    public class RBMKConsoleBuild extends Building {
        public int targetX;
        public int targetY;
        public boolean linked;
        public boolean centerMode;
        public boolean[][] sel = new boolean[16][16];
        public Column[] view = new Column[256];
        public float[] fluxBuf = new float[60];
        private int fluxStep;
        public boolean azArmed;
        public float azDeathTime;
        private float lastUIScan;
        private float lastTipScan;
        private final Table tooltip = new Table().background(Styles.black6);
        private int hoverCell = -1;

        public RBMKConsoleBuild() {
            for (int i = 0; i < this.view.length; ++i) {
                this.view[i] = new Column();
            }
        }

        public float fluxOut() {
            float m = 0.0f;
            for (float f : this.fluxBuf) {
                if (!(f > m)) continue;
                m = f;
            }
            return m;
        }

        public float avgColumnHeat() {
            float sum = 0.0f;
            int n = 0;
            for (Column c : this.view) {
                if (c.type == -1) continue;
                sum += c.heat;
                ++n;
            }
            return n == 0 ? 25.0f : sum / (float)n;
        }

        public String statusText() {
            float avg = this.avgColumnHeat();
            if (avg < 100.0f) {
                return "Cold";
            }
            if (avg < 500.0f) {
                return "Stable";
            }
            if (avg < 800.0f) {
                return "Hot";
            }
            return "Critical";
        }

        public Color statusColor() {
            float avg = this.avgColumnHeat();
            if (avg < 100.0f) {
                return Color.blue;
            }
            if (avg < 500.0f) {
                return Color.green;
            }
            if (avg < 800.0f) {
                return Color.yellow;
            }
            return Color.red;
        }

        public void onProximityAdded() {
            super.onProximityAdded();
            this.rescan();
        }

        public void updateTile() {
            if (Vars.state.tick % 10.0 == 0.0) {
                this.rescan();
            }
            if (this.azArmed && Time.time >= this.azDeathTime) {
                this.azArmed = false;
            }
        }

        void setCenter(int x, int y) {
            this.targetX = x;
            this.targetY = y;
            this.linked = true;
            this.centerMode = false;
            this.rescan();
        }

        private Point2 indexToWorld(int index) {
            int x = index % 16 - 8;
            int y = index / 16 - 8;
            return new Point2(this.targetX + x * 2, this.targetY + y * 2);
        }

        protected void rescan() {
            if (!this.linked) {
                for (int i = 0; i < 256; ++i) {
                    this.view[i].type = (byte)-1;
                    this.view[i].heat = 0.0f;
                    this.view[i].maxHeat = 0.0f;
                }
                return;
            }
            float fluxTotal = 0.0f;
            for (int index = 0; index < 256; ++index) {
                Point2 p = this.indexToWorld(index);
                Column c = this.view[index];
                Building b = Vars.world.build(p.x, p.y);
                if (b instanceof RBMKBase.RBMKBaseBuild) {
                    RBMKBase.RBMKBaseBuild rb = (RBMKBase.RBMKBaseBuild)b;
                    c.type = (byte)rb.getConsoleType().ordinal();
                    c.heat = rb.heat;
                    c.maxHeat = rb.maxHeat();
                    c.moderated = rb.isModerated();
                    c.level = -1.0f;
                    c.color = -1;
                    c.enrichment = -1.0f;
                    c.xenon = -1.0f;
                    c.coreHeat = -1.0f;
                    c.hullHeat = -1.0f;
                    c.water = -1.0f;
                    c.steam = -1.0f;
                    c.steamTier = -1;
                    if (rb instanceof RBMKControl.RBMKControlBuild) {
                        RBMKControl.RBMKControlBuild ct = (RBMKControl.RBMKControlBuild)rb;
                        c.level = (float)ct.level;
                        c.color = ct.color;
                    }
                    if (rb instanceof RBMKRod.RBMKRodBuild) {
                        RBMKRod.RBMKRodBuild rd = (RBMKRod.RBMKRodBuild)rb;
                        if (rd.fuelState != null && rd.fuelItem != null) {
                            c.enrichment = (float)rd.fuelState.getEnrichment(rd.fuelItem);
                            c.xenon = (float)rd.fuelState.getPoisonLevel();
                            c.coreHeat = (float)rd.fuelState.coreHeat;
                            c.hullHeat = (float)rd.fuelState.hullHeat;
                        }
                    }
                    if (rb instanceof RBMKBoiler.RBMKBoilerBuild) {
                        RBMKBoiler.RBMKBoilerBuild bl = (RBMKBoiler.RBMKBoilerBuild)rb;
                        c.water = bl.liquids.get(Liquids.water);
                        c.steam = bl.liquids.get(bl.currentSteam());
                        c.steamTier = bl.steamTier + 1;
                    }
                    fluxTotal = (float)((double)fluxTotal + rb.consoleFlux());
                } else {
                    c.type = (byte)-1;
                    c.heat = 0.0f;
                    c.maxHeat = 0.0f;
                }
                if (!this.sel[index / 16][index % 16] || b instanceof RBMKBase.RBMKBaseBuild) continue;
                this.sel[index / 16][index % 16] = false;
            }
            this.fluxStep = (this.fluxStep + 1) % 64;
            if (this.fluxStep == 0) {
                for (int k = 0; k < this.fluxBuf.length - 1; ++k) {
                    this.fluxBuf[k] = this.fluxBuf[k + 1];
                }
                this.fluxBuf[this.fluxBuf.length - 1] = fluxTotal;
            }
        }

        private RBMKBase.RBMKBaseBuild rbmkAt(int i, int j) {
            Point2 p = this.indexToWorld(i * 16 + j);
            Building b = Vars.world.build(p.x, p.y);
            return b instanceof RBMKBase.RBMKBaseBuild ? (RBMKBase.RBMKBaseBuild)b : null;
        }

        public boolean onConfigureBuildTapped(Building other) {
            if (this.centerMode && other != null) {
                this.configure(new Point2(other.tileX(), other.tileY()));
                return false;
            }
            return super.onConfigureBuildTapped(other);
        }

        public void handleCode(int code) {
            if (code >= 1000 && code < 1256) {
                int idx = code - 1000;
                this.sel[idx / 16][idx % 16] = !this.sel[idx / 16][idx % 16];
                return;
            }
            if (code >= 3000 && code < 3005) {
                int col = code - 3000;
                boolean anySel = false;
                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        RBMKBase.RBMKBaseBuild rb = this.rbmkAt(i, j);
                        if (!this.sel[i][j] || !(rb instanceof RBMKControl.RBMKControlBuild)) continue;
                        RBMKControl.RBMKControlBuild ct = (RBMKControl.RBMKControlBuild)rb;
                        if (ct.getConsoleType() != RBMKBase.ColumnType.CONTROL) continue;
                        anySel = true;
                        break;
                    }
                }
                if (anySel) {
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            RBMKBase.RBMKBaseBuild rb = this.rbmkAt(i, j);
                            if (!this.sel[i][j] || !(rb instanceof RBMKControl.RBMKControlBuild)) continue;
                            RBMKControl.RBMKControlBuild ct = (RBMKControl.RBMKControlBuild)rb;
                            if (ct.getConsoleType() != RBMKBase.ColumnType.CONTROL) continue;
                            ct.setPacked(RBMKControl.RBMKControlBuild.pack((int)col, (int)((int)(ct.targetLevel * 100.0))));
                        }
                    }
                } else {
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            Column c = this.view[i * 16 + j];
                            this.sel[i][j] = c.type == RBMKBase.ColumnType.CONTROL.ordinal() && c.color == col;
                        }
                    }
                }
                return;
            }
            if (code >= 4000 && code < 4101) {
                double lvl = (double)(code - 4000) / 100.0;
                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        RBMKBase.RBMKBaseBuild rb = this.rbmkAt(i, j);
                        if (!this.sel[i][j] || !(rb instanceof RBMKControl.RBMKControlBuild)) continue;
                        RBMKControl.RBMKControlBuild ct = (RBMKControl.RBMKControlBuild)rb;
                        ct.setTarget(lvl);
                    }
                }
                return;
            }
            if (code >= 5000 && code < 5005) {
                int tier = code - 5000;
                if (tier < 1 || tier > 4) {
                    return;
                }
                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        RBMKBase.RBMKBaseBuild rb = this.rbmkAt(i, j);
                        if (!this.sel[i][j] || !(rb instanceof RBMKBoiler.RBMKBoilerBuild)) continue;
                        RBMKBoiler.RBMKBoilerBuild bl = (RBMKBoiler.RBMKBoilerBuild)rb;
                        bl.setSteamTier(tier - 1);
                    }
                }
                return;
            }
            switch (code) {
                case 2000:
                case 2002: {
                    int ctrl = RBMKBase.ColumnType.CONTROL.ordinal();
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            this.sel[i][j] = this.view[i * 16 + j].type == ctrl;
                        }
                    }
                    break;
                }
                case 2001: {
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            this.sel[i][j] = false;
                        }
                    }
                    break;
                }
                case 6000: {
                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            RBMKBase.RBMKBaseBuild rb = this.rbmkAt(i, j);
                            if (!(rb instanceof RBMKControl.RBMKControlBuild)) continue;
                            RBMKControl.RBMKControlBuild ct = (RBMKControl.RBMKControlBuild)rb;
                            ct.setTarget(0.0);
                        }
                    }
                    break;
                }
            }
        }

        public void buildConfiguration(Table table) {
            Table cont = new Table().top().left();
            cont.defaults().left();

            // 顶部横排：标题 | 中心设置
            cont.table(Styles.grayPanel, info -> {
                info.left().defaults().left();
                info.add("[accent]RBMK Console[]").row();
                info.image().color(Pal.accent).growX().height(2.0f).pad(2.0f).row();
            }).pad(10.0f);
            cont.add().width(12.0f);
            cont.table(Styles.grayPanel, center -> {
                center.left().defaults().left();
                Label cLabel = new Label(() -> this.linked ? "[green]Center: [" + this.targetX + ", " + this.targetY + "[]" : "[red]Not connected[]");
                center.add(cLabel).row();
                center.add("[gray]Enable 'Set Center' then click any column to link.[]").row();
                TextButton setCenter = new TextButton(this.centerMode ? "[yellow]Click target...[]" : "Set Center", Styles.flatt);
                setCenter.clicked(() -> {
                    this.centerMode = !this.centerMode;
                    setCenter.setText(this.centerMode ? "[yellow]Click target...[]" : "Set Center");
                });
                center.add(setCenter).size(110.0f, 34.0f).padTop(4.0f);
            }).pad(12.0f);
            cont.row();

            // 中部横排：结构图 | 控制面板组
            cont.table(Styles.grayPanel, structure -> {
                structure.left().defaults().left();
                structure.add("[accent]Reactor Structure:[gray] click to select, hover for info[]").row();
                structure.add(this.buildBody()).pad(6.0f);
            }).pad(12.0f);
            cont.add().width(12.0f);
            Table controls = new Table().top().left();
            controls.defaults().left();
            controls.table(Styles.grayPanel, act -> {
                act.left().defaults().left();
                TextButton all = new TextButton("Select All Manual Rods", Styles.flatt);
                all.clicked(() -> this.configure(2002));
                act.add(all).size(170.0f, 32.0f).pad(3.0f);
                TextButton clear = new TextButton("Clear Selection", Styles.flatt);
                clear.clicked(() -> this.configure(2001));
                act.add(clear).size(130.0f, 32.0f).pad(3.0f);
            }).growX().pad(4.0f).row();
            controls.table(Styles.grayPanel, grp -> {
                grp.left().defaults().left();
                grp.add("[accent]Control Rod Grouping:[gray] auto excluded").row();
                Table row = new Table();
                for (int g = 0; g < 5; ++g) {
                    int gg = g;
                    Button b = new Button(Styles.cleari);
                    Image im = new Image(Tex.whiteui);
                    im.setColor(GROUP_COLORS[gg]);
                    b.add(im).size(34.0f);
                    b.clicked(() -> this.configure(3000 + gg));
                    row.add(b).size(34.0f).pad(3.0f);
                }
                grp.add(row).pad(4.0f).row();
                grp.add("[gray]Assign color to selected / select all of a color.[]").left().pad(2.0f);
            }).growX().pad(4.0f).row();
            controls.table(Styles.grayPanel, rod -> {
                rod.left().defaults().left();
                rod.add("[accent]Control Rod Height:[gray] 0-100").row();
                Table row = new Table();
                TextField field = new TextField("50", Styles.defaultField);
                field.setFilter(TextField.TextFieldFilter.digitsOnly);
                field.setMaxLength(3);
                row.add(field).width(80.0f).pad(2.0f);
                TextButton apply = new TextButton("Apply", Styles.defaultt);
                apply.clicked(() -> {
                    int val = RBMKConsole.clampInt(RBMKConsole.parseInt(field.getText(), 0), 100);
                    this.configure(4000 + val);
                });
                row.add(apply).size(70.0f, 32.0f).pad(2.0f);
                rod.add(row).pad(4.0f);
                rod.add("[gray]Applies to selected manual rods.[]").left().pad(2.0f);
            }).growX().pad(4.0f).row();
            controls.table(Styles.grayPanel, boiler -> {
                boiler.left().defaults().left();
                boiler.add("[accent]Boiler Steam Tier:[gray] 1-4 (STEAM/HOT/SUPERHOT/ULTRAHOT)").row();
                Table row = new Table();
                TextField tf = new TextField("1", Styles.defaultField);
                tf.setFilter(TextField.TextFieldFilter.digitsOnly);
                tf.setMaxLength(1);
                row.add(tf).width(60.0f).pad(2.0f);
                TextButton apply = new TextButton("Apply", Styles.defaultt);
                apply.clicked(() -> {
                    int v = RBMKConsole.parseInt(tf.getText(), 1);
                    if (v < 1 || v > 4) {
                        return;
                    }
                    this.configure(5000 + v);
                });
                row.add(apply).size(70.0f, 32.0f).pad(2.0f);
                boiler.add(row).pad(4.0f);
                boiler.add("[gray]Applied to selected boilers.[]").left().pad(2.0f);
            }).growX().pad(4.0f).row();
            cont.add(controls).pad(12.0f);
            cont.row();

            // 底部横排：概览(+紧急) | 通量图
            cont.table(Styles.grayPanel, overview -> {
                overview.left().defaults().left();
                overview.add("[accent]Overview:[]").row();
                overview.add(this.statusLabel()).growX().pad(2.0f).row();
                overview.add("[accent]Emergency Controls:[]").padTop(6.0f).row();
                overview.add(this.buildAZ5()).pad(2.0f);
            }).pad(12.0f).padTop(-100.0f);
            cont.add().width(12.0f);
            cont.table(Styles.grayPanel, flux -> {
                flux.left().defaults().left();
                flux.add("[accent]Neutron Flux History:[]").row();
                flux.add(this.buildFluxChart()).height(210.0f).width(520.0f).pad(4.0f).row();
            }).pad(12.0f);
            cont.row();
            Table main = new Table().background(Styles.black6);
            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, true);
            main.add(pane).maxWidth(1200.0f).maxHeight(730.0f);
            table.add(main);
            main.update(() -> {
                if (Time.time >= this.lastUIScan) {
                    this.lastUIScan = Time.time + 0.15f;
                    this.rescan();
                }
            });
        }

        private Label statusLabel() {
            Label l = new Label(() -> {
                float avg = this.avgColumnHeat();
                int components = 0;
                int fuel = 0;
                int ctrl = 0;
                float xenSum = 0.0f;
                float depSum = 0.0f;
                float coreSum = 0.0f;
                float hullSum = 0.0f;
                int xenN = 0;
                int depN = 0;
                int coreN = 0;
                int hullN = 0;
                for (Column c : this.view) {
                    if (c.type == -1) continue;
                    ++components;
                    if (c.type == RBMKBase.ColumnType.FUEL.ordinal() || c.type == RBMKBase.ColumnType.FUEL_SIM.ordinal()) {
                        ++fuel;
                    }
                    if (c.type == RBMKBase.ColumnType.CONTROL.ordinal() || c.type == RBMKBase.ColumnType.CONTROL_AUTO.ordinal()) {
                        ++ctrl;
                    }
                    if (c.xenon >= 0.0f) {
                        xenSum += c.xenon;
                        ++xenN;
                    }
                    if (c.enrichment >= 0.0f) {
                        depSum += 100.0f - c.enrichment * 100.0f;
                        ++depN;
                    }
                    if (c.coreHeat >= 0.0f) {
                        coreSum += c.coreHeat;
                        ++coreN;
                    }
                    if (c.hullHeat >= 0.0f) {
                        hullSum += c.hullHeat;
                        ++hullN;
                    }
                }
                return "Status: [accent]" + this.statusText() + "[]  Temp: " + (int)avg + "\u00b0C\nFlux: " + (int)this.fluxOut() + "  Xenon: " + (xenN == 0 ? "--" : (int)(xenSum / (float)xenN * 100.0f) + "%") + "  Depletion: " + (depN == 0 ? "--" : (int)(depSum / (float)depN) + "%") + "\nCore: " + (coreN == 0 ? "--" : (int)(coreSum / (float)coreN) + "\u00b0C") + "  Hull: " + (hullN == 0 ? "--" : (int)(hullSum / (float)hullN) + "\u00b0C") + "\nComponents: " + components + "  Fuel: " + fuel + "  Control: " + ctrl;
            });
            return l;
        }

        private String tipText(int index) {
            Column c = this.view[index];
            StringBuilder sb = new StringBuilder();
            if (c.type == -1) {
                sb.append("[gray]Empty[]");
                return sb.toString();
            }
            RBMKBase.ColumnType t = RBMKBase.ColumnType.values()[c.type];
            sb.append("[accent]").append(t.name()).append("[]");
            sb.append("  [gray]").append((int)c.heat).append("\u00b0C[]\n");
            sb.append("Heat: ").append((int)c.heat).append(" / ").append((int)c.maxHeat).append("\u00b0C");
            if (c.moderated) {
                sb.append("  [cyan]Moderated[]");
            }
            if (c.level >= 0.0f) {
                sb.append("\nLevel: ").append((int)(c.level * 100.0f)).append("%");
                if (c.color >= 0 && c.color < GROUP_NAMES.length) {
                    sb.append("  Group: ").append(GROUP_NAMES[c.color]);
                }
            }
            if (c.enrichment >= 0.0f) {
                sb.append("\nEnrichment: ").append((int)(c.enrichment * 100.0f)).append("%");
                sb.append("  Xenon: ").append((int)(c.xenon * 100.0f)).append("%");
                sb.append("\nCore: ").append((int)c.coreHeat).append("\u00b0C");
                sb.append("  Hull: ").append((int)c.hullHeat).append("\u00b0C");
            }
            if (c.water >= 0.0f) {
                sb.append("\nWater: ").append((int)c.water);
                sb.append("  Steam: ").append((int)c.steam);
                sb.append("  Tier: ").append(c.steamTier);
            }
            return sb.toString();
        }

        private void refreshTip(int index, Element cell) {
            this.hoverCell = index;
            this.tooltip.clearChildren();
            this.tooltip.add(new Label(this.tipText(index))).pad(6.0f);
            this.tooltip.pack();
            this.tooltip.visible = true;
            this.tooltip.toFront();
            this.moveTip(cell);
        }

        private void showTip(int index, Element cell) {
            if (this.hoverCell != index) {
                this.refreshTip(index, cell);
            }
        }

        private void moveTip(Element cell) {
            if (this.hoverCell < 0) {
                return;
            }
            if (cell.getScene() != null && this.tooltip.parent == null) {
                cell.getScene().root.addChild(this.tooltip);
            }
            Tmp.v1.set(cell.x, cell.y);
            cell.localToStageCoordinates(Tmp.v1);
            this.tooltip.setPosition(Tmp.v1.x, Tmp.v1.y + cell.getHeight() + 4.0f);
        }

        private void hideTip() {
            this.hoverCell = -1;
            this.tooltip.visible = false;
        }

        private Table buildBody() {
            Table t = new Table();
            for (int i = 15; i >= 0; --i) {
                for (int j = 0; j < 16; ++j) {
                    final int fi = i, fj = j;
                    final int idx = fi * 16 + fj;
                    Image cell = new Image(Tex.whiteui);
                    cell.update(() -> {
                        Column c = this.view[idx];
                        if (c.type == -1) {
                            cell.setColor(Color.darkGray);
                        } else if (this.sel[fi][fj]) {
                            Tmp.c1.set(RBMKConsole.typeColor(c.type, c.heat, c.maxHeat)).lerp(Color.white, 0.5f);
                            cell.setColor(Tmp.c1);
                        } else {
                            cell.setColor(RBMKConsole.typeColor(c.type, c.heat, c.maxHeat));
                        }
                        if (this.hoverCell == idx) {
                            this.moveTip(cell);
                            if (Time.time >= this.lastTipScan) {
                                this.lastTipScan = Time.time + 0.15f;
                                this.refreshTip(idx, cell);
                            }
                        }
                    });
                    cell.clicked(() -> this.configure(1000 + idx));
                    cell.hovered(() -> this.showTip(idx, cell));
                    cell.exited(this::hideTip);
                    Element heat = this.heatOverlay(idx);
                    heat.toFront();
                    t.stack(new Element[]{cell, heat}).size(16.0f).pad(1.0f);
                }
                t.row();
            }
            return t;
        }

        private Element heatOverlay(final int idx) {
            return new Element() {
                {
                    this.touchable(() -> Touchable.disabled);
                    this.setFillParent(false);
                }

                public void draw() {
                    Column c = RBMKConsoleBuild.this.view[idx];
                    if (c.type == -1 || c.maxHeat <= 0.0f) {
                        return;
                    }
                    float w = this.getWidth();
                    float h = this.getHeight();
                    float x0 = this.x;
                    float y0 = this.y;

                    // 左侧温度竖条：自底部生长，高度按 (heat-20)/maxHeat 归一
                    float frac = Mathf.clamp((float)((c.heat - 20.0f) / c.maxHeat));
                    if (frac >= 0.02f) {
                        float barH = h * frac;
                        int slices = 8;
                        float sh = barH / (float)slices;
                        for (int i = 0; i < slices; ++i) {
                            float t = 1.0f - (float)i / (float)(slices - 1);
                            Draw.color((float)(0.9f - 0.25f * t), (float)(0.15f + 0.45f * t), (float)(0.08f + 0.1f * t), (float)(0.55f + 0.45f * (1.0f - t)));
                            Fill.rect((float)(x0 + 1.5f), (float)(y0 + ((float)i + 0.5f) * sh), 3.0f, sh);
                        }
                    }

                    // 组件专用指示条（同 HBM：底部/顶部窄竖条）
                    RBMKBase.ColumnType type = RBMKBase.ColumnType.values()[c.type];
                    if (type == RBMKBase.ColumnType.FUEL || type == RBMKBase.ColumnType.FUEL_SIM || type == RBMKBase.ColumnType.BREEDER) {
                        if (c.coreHeat >= 0.0f) {
                            float fh = (float)Math.ceil((c.coreHeat - 20.0f) * (h - 4.0f) / c.maxHeat);
                            fh = Mathf.clamp((float)fh, 0.0f, h - 4.0f);
                            Draw.color((float)0.9f, (float)0.3f, (float)0.2f);
                            Fill.rect((float)(x0 + 5.5f), (float)(y0 + 2.0f + fh / 2.0f), 2.0f, fh); // 棒芯热
                        }
                        if (c.enrichment >= 0.0f) {
                            float fe = c.enrichment * (h - 4.0f);
                            Draw.color((float)0.3f, (float)0.9f, (float)0.3f);
                            Fill.rect((float)(x0 + 9.5f), (float)(y0 + 2.0f + fe / 2.0f), 2.0f, fe); // 燃料消耗(富集度剩余)
                        }
                        if (c.xenon >= 0.0f) {
                            float fx = c.xenon * (h - 4.0f);
                            Draw.color((float)0.6f, (float)0.55f, (float)0.8f);
                            Fill.rect((float)(x0 + 13.5f), (float)(y0 + 2.0f + fx / 2.0f), 2.0f, fx); // 氙毒
                        }
                    } else if (type == RBMKBase.ColumnType.CONTROL || type == RBMKBase.ColumnType.CONTROL_AUTO) {
                        if (c.level >= 0.0f) {
                            // 控制棒位置：从顶部向下，level=0(全插入)长条，level=1(全抽出)消失
                            float fr = (1.0f - Mathf.clamp((float)c.level, 0.0f, 1.0f)) * (h - 2.0f);
                            Draw.color((float)0.95f, (float)0.8f, (float)0.2f);
                            Fill.rect((float)(x0 + w / 2.0f), (float)(y0 + h - fr / 2.0f), 3.0f, fr); // 位置指示条
                        }
                    } else if (type == RBMKBase.ColumnType.BOILER) {
                        if (c.water >= 0.0f) {
                            // 存水量：自底部生长，按 feedCapacity 归一
                            float fw = Mathf.clamp((float)(c.water / 100.0f)) * (h - 4.0f);
                            Draw.color((float)0.25f, (float)0.55f, (float)0.95f);
                            Fill.rect((float)(x0 + 5.5f), (float)(y0 + 2.0f + fw / 2.0f), 2.0f, fw); // 水
                        }
                        if (c.steam >= 0.0f) {
                            // 蒸汽量：自底部生长，按 steamCapacity 归一
                            float fs = Mathf.clamp((float)(c.steam / 10000.0f)) * (h - 4.0f);
                            Draw.color((float)0.8f, (float)0.85f, (float)0.9f);
                            Fill.rect((float)(x0 + 9.5f), (float)(y0 + 2.0f + fs / 2.0f), 2.0f, fs); // 蒸汽
                        }
                        if (c.steamTier > 0) {
                            // 蒸汽等级：从顶部向下小标记，1-4 级递增下移
                            float fy = 1.0f + 2.0f * Mathf.clamp((float)(c.steamTier - 1), 0.0f, 3.0f);
                            Draw.color((float)0.95f, (float)0.7f, (float)0.25f);
                            Fill.rect((float)(x0 + 13.5f), (float)(y0 + h - fy - 1.0f), 2.0f, 2.0f); // 等级标记
                        }
                    }

                    // 选中红框
                    if (RBMKConsoleBuild.this.sel[idx / 16][idx % 16]) {
                        Draw.color(Color.red);
                        Lines.stroke((float)1.7f);
                        Lines.rect((float)(x0 + 0.5f), (float)(y0 + 0.5f), w - 1.0f, h - 1.0f);
                        Draw.reset();
                    }
                    Draw.color();
                }
            };
        }

        private Element buildFluxChart() {
            return new Element() {
                public void draw() {
                    float w = this.getWidth();
                    float h = this.getHeight();
                    float gx = this.x;
                    float gy = this.y;
                    Draw.color((float)0.08f, (float)0.18f, (float)0.08f);
                    Fill.rect((float)(gx + w / 2.0f), (float)(gy + h / 2.0f), (float)w, (float)h);
                    Draw.color((float)0.3f, (float)0.6f, (float)0.3f);
                    Lines.stroke((float)1.0f);
                    for (int i = 0; i <= 8; ++i) {
                        Lines.line((float)(gx + w * (float)i / 8.0f), (float)gy, (float)(gx + w * (float)i / 8.0f), (float)(gy + h));
                    }
                    for (int j = 0; j <= 4; ++j) {
                        Lines.line((float)gx, (float)(gy + h * (float)j / 4.0f), (float)(gx + w), (float)(gy + h * (float)j / 4.0f));
                    }
                    float max = 1.0f;
                    float min = Float.MAX_VALUE;
                    for (float f : RBMKConsoleBuild.this.fluxBuf) {
                        max = Math.max(max, f);
                        min = Math.min(min, f);
                    }
                    if (min == Float.MAX_VALUE) {
                        min = 0.0f;
                    }
                    Draw.color(Color.lime);
                    Lines.stroke((float)2.0f);
                    for (int i = 1; i < RBMKConsoleBuild.this.fluxBuf.length; ++i) {
                        float x1 = gx + (float)(i - 1) / (float)(RBMKConsoleBuild.this.fluxBuf.length - 1) * w;
                        float y1 = gy + RBMKConsoleBuild.this.fluxBuf[i - 1] / max * h;
                        float x2 = gx + (float)i / (float)(RBMKConsoleBuild.this.fluxBuf.length - 1) * w;
                        float y2 = gy + RBMKConsoleBuild.this.fluxBuf[i] / max * h;
                        Lines.line((float)x1, (float)y1, (float)x2, (float)y2);
                    }
                    Draw.color();
                    String maxS = "" + (int)max;
                    String minS = "" + (int)min;
                    Fonts.outline.setColor(Color.lime);
                    Fonts.outline.draw((CharSequence)maxS, gx + 2.0f, gy + h - 1.0f, 8);
                    Fonts.outline.draw((CharSequence)maxS, gx + w - 2.0f, gy + h - 1.0f, 16);
                    Fonts.outline.draw((CharSequence)minS, gx + 2.0f, gy + 0.0f, 8);
                    Fonts.outline.draw((CharSequence)minS, gx + w - 2.0f, gy + 0.0f, 16);
                    Fonts.outline.setColor(Color.white);
                }
            };
        }

        private Table buildAZ5() {
            Table t = new Table();
            TextButton az = new TextButton("AZ-5 SCRAM", new TextButton.TextButtonStyle(Styles.flatt));
            Element border = new Element() {
                public void draw() {
                    if (!RBMKConsoleBuild.this.azArmed) {
                        return;
                    }
                    float w = this.getWidth();
                    float h = this.getHeight();
                    Draw.color(Color.red, (float)0.9f);
                    Lines.stroke((float)3.0f);
                    Lines.rect((float)this.x, (float)this.y, (float)w, (float)h);
                    Draw.reset();
                }
            };
            border.touchable(() -> Touchable.disabled);
            border.visible = false;
            az.update(() -> {
                border.visible = this.azArmed;
                if (this.azArmed) {
                    float left = Math.max(0.0f, (this.azDeathTime - Time.time) / 60.0f);
                    az.setText("[red]CONFIRM! " + Mathf.ceil((float)left) + "s[]");
                } else {
                    az.setText("AZ-5 SCRAM");
                }
            });
            az.clicked(() -> {
                if (!this.azArmed) {
                    this.azArmed = true;
                    this.azDeathTime = Time.time + 300.0f;
                } else {
                    this.azArmed = false;
                    this.configure(6000);
                }
            });
            t.stack(new Element[]{az, border}).size(180.0f, 44.0f);
            return t;
        }

        // ---------- 方块表面结构图（复用 view[] 扫描快照，仅显示、不参与选择交互） ----------

        @Override
        public void draw() {
            super.draw();
            drawStructureOverlay();
        }

        /** 在方块表面绘制 16×16 结构图，逐格渲染与 UI heatOverlay 完全一致（含温度条/组件指示条，无选择框） */
        public void drawStructureOverlay() {
            float panelSize = block.size * Vars.tilesize - 5f;// 这是在方块面上绘制的结构图留给边框的px大小
            float cellSize = panelSize / GRID;
            float px = x, py = y;
            float s = cellSize / 16f; // UI 16px 格 → 方块格的缩放系数

            Draw.z(Layer.blockOver);

            // 背景
            Draw.color(Color.darkGray);
            Fill.rect(px, py, panelSize, panelSize);

            for (int i = 0; i < CELLS; i++) {
                Column c = view[i];
                int gx = i % GRID;
                int gy = i / GRID;

                // 本格左下角与中心
                float bx = px - panelSize / 2f + cellSize * gx;
                float by = py - panelSize / 2f + cellSize * gy;
                float cx = bx + cellSize / 2f;
                float cy = by + cellSize / 2f;
                float h = cellSize;

                if (c.type == -1) {
                    Draw.color(Color.darkGray);
                    Fill.rect(cx, cy, cellSize, cellSize);
                    continue;
                }

                // 基色 = 类型颜色（与 UI buildBody 相同）
                Draw.color(typeColor(c.type, c.heat, c.maxHeat));
                Fill.rect(cx, cy, cellSize, cellSize);

                if (c.maxHeat <= 0f) continue;

                // 左侧温度竖条：自底部生长，红黄渐变分片（与 UI heatOverlay 逐项一致）
                float frac = Mathf.clamp((c.heat - 20f) / c.maxHeat);
                if (frac >= 0.02f) {
                    float barH = h * frac;
                    int slices = 8;
                    float sh = barH / slices;
                    for (int k = 0; k < slices; k++) {
                        float t = 1f - k / (float) (slices - 1);
                        Draw.color(0.9f - 0.25f * t, 0.15f + 0.45f * t, 0.08f + 0.1f * t, 0.55f + 0.45f * (1f - t));
                        Fill.rect(bx + 1.5f * s, by + (k + 0.5f) * sh, 3f * s, sh);
                    }
                }

                RBMKBase.ColumnType type = RBMKBase.ColumnType.values()[c.type];

                if (type == RBMKBase.ColumnType.FUEL || type == RBMKBase.ColumnType.FUEL_SIM || type == RBMKBase.ColumnType.BREEDER) {
                    if (c.coreHeat >= 0f) {
                        float fh = Mathf.clamp((float) Math.ceil((c.coreHeat - 20f) * (h - 4f * s) / c.maxHeat), 0f, h - 4f * s);
                        Draw.color(0.9f, 0.3f, 0.2f);
                        Fill.rect(bx + 5.5f * s, by + 2f * s + fh / 2f, 2f * s, fh);
                    }
                    if (c.enrichment >= 0f) {
                        float fe = Mathf.clamp(c.enrichment, 0f, 1f) * (h - 4f * s);
                        Draw.color(0.3f, 0.9f, 0.3f);
                        Fill.rect(bx + 9.5f * s, by + 2f * s + fe / 2f, 2f * s, fe);
                    }
                    if (c.xenon >= 0f) {
                        float fx = Mathf.clamp(c.xenon, 0f, 1f) * (h - 4f * s);
                        Draw.color(0.6f, 0.55f, 0.8f);
                        Fill.rect(bx + 13.5f * s, by + 2f * s + fx / 2f, 2f * s, fx);
                    }
                } else if (type == RBMKBase.ColumnType.CONTROL || type == RBMKBase.ColumnType.CONTROL_AUTO) {
                    if (c.level >= 0f) {
                        float fr = (1f - Mathf.clamp(c.level, 0f, 1f)) * (h - 2f * s);
                        Draw.color(0.95f, 0.8f, 0.2f);
                        Fill.rect(cx, by + h - fr / 2f, 3f * s, fr);
                    }
                } else if (type == RBMKBase.ColumnType.BOILER) {
                    if (c.water >= 0f) {
                        float fw = Mathf.clamp(c.water / 100f) * (h - 4f * s);
                        Draw.color(0.25f, 0.55f, 0.95f);
                        Fill.rect(bx + 5.5f * s, by + 2f * s + fw / 2f, 2f * s, fw);
                    }
                    if (c.steam >= 0f) {
                        float fs = Mathf.clamp(c.steam / 10000f) * (h - 4f * s);
                        Draw.color(0.8f, 0.85f, 0.9f);
                        Fill.rect(bx + 9.5f * s, by + 2f * s + fs / 2f, 2f * s, fs);
                    }
                    if (c.steamTier > 0) {
                        float fy = 1f * s + 2f * s * Mathf.clamp(c.steamTier - 1, 0, 3);
                        Draw.color(0.95f, 0.7f, 0.25f);
                        Fill.rect(bx + 13.5f * s, by + h - fy - 1f * s, 2f * s, 2f * s);
                    }
                }
            }

            // 绿色网格线覆盖层（与 UI 通量图的网格风格一致）
            Draw.color(0.3f, 0.6f, 0.3f);
            Lines.stroke(0.25f);
            float x0 = px - panelSize / 2f, x1 = px + panelSize / 2f;
            float y0 = py - panelSize / 2f, y1 = py + panelSize / 2f;
            for (int i = 0; i <= GRID; i++) {
                float posX = x0 + cellSize * i;
                Lines.line(posX, y0, posX, y1); // 竖线
                float posY = y0 + cellSize * i;
                Lines.line(x0, posY, x1, posY); // 横线
            }
            Draw.color();

            // 控制台在本网格中的位置标记（以中心格估算）
            if (linked) {
                int cgx = (tileX() + block.size / 2 - targetX) / 2 + GRID / 2;
                int cgy = (tileY() + block.size / 2 - targetY) / 2 + GRID / 2;
                if (cgx >= 0 && cgx < GRID && cgy >= 0 && cgy < GRID) {
                    float cx = px - panelSize / 2f + cellSize * cgx + cellSize / 2f;
                    float cy = py - panelSize / 2f + cellSize * cgy + cellSize / 2f;
                    Draw.color(Color.white);
                    Lines.stroke(1.5f);
                    Lines.line(cx - 2f, cy, cx + 2f, cy);
                    Lines.line(cx, cy - 2f, cx, cy + 2f);
                    Draw.color();
                }
            }
        }

        /** 序列化版本：1 起新增选中态/中心模式/AZ-5/通量环形指针等持久字段 */
        @Override
        public byte version() {
            return 1;
        }

        public void write(Writes write) {
            super.write(write);
            write.i(this.targetX);
            write.i(this.targetY);
            write.bool(this.linked);
            for (float f : this.fluxBuf) {
                write.f(f);
            }
            // 版本 1：选中标记（16×16 打包为 32 字节）+ 中心设置模式 + AZ-5 状态 + 通量环形指针
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j += 8) {
                    byte packed = 0;
                    for (int k = 0; k < 8; k++) {
                        if (this.sel[i][j + k]) packed |= (byte) (1 << k);
                    }
                    write.b(packed);
                }
            }
            write.bool(this.centerMode);
            write.bool(this.azArmed);
            write.f(this.azDeathTime);
            write.i(this.fluxStep);
        }

        public void read(Reads read, byte revision) {
            super.read(read, revision);
            this.targetX = read.i();
            this.targetY = read.i();
            this.linked = read.bool();
            for (int i = 0; i < this.fluxBuf.length; ++i) {
                this.fluxBuf[i] = read.f();
            }
            if (revision >= 1) {
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j += 8) {
                        byte packed = read.b();
                        for (int k = 0; k < 8; k++) {
                            this.sel[i][j + k] = (packed & (1 << k)) != 0;
                        }
                    }
                }
                this.centerMode = read.bool();
                this.azArmed = read.bool();
                this.azDeathTime = read.f();
                this.fluxStep = read.i();
            }
        }
    }

    public static class Column {
        public byte type = (byte)-1;
        public float heat;
        public float maxHeat;
        public boolean moderated;
        public float level = -1.0f;
        public int color = -1;
        public float enrichment = -1.0f;
        public float xenon = -1.0f;
        public float coreHeat = -1.0f;
        public float hullHeat = -1.0f;
        public float water = -1.0f;
        public float steam = -1.0f;
        public int steamTier = -1;
    }
}
