package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.expand.world.block.power.RBMKRod.RBMKRodBuild;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * RBMK控制台
 * 监控和控制整个反应堆
 */
public class RBMKConsole extends RBMKBase{
    public float scanRange = 20f;
    
    // 中子通量显示缓冲区大小
    public static final int fluxDisplayBuffer = 60;
    
    public RBMKConsole(String name){
        super(name);
        size = 12;
        hasItems = false;
        configurable = true;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 1000,
            Items.lead, 800,
            Items.silicon, 300,
            Items.titanium, 500,
            Items.thorium, 100
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Monitors and controls reactor");
        stats.add(Stat.range, scanRange / tilesize, StatUnit.blocks);
    }
    
    @Override
    public void setBars(){
        super.setBars();
        addBar("neutronFlux", (RBMKConsoleBuild entity) -> new Bar(
            () -> "Neutron Flux: " + (int)entity.neutronFlux,
            () -> Pal.accent,
            () -> Mathf.clamp(entity.neutronFlux / 6000f, 0f, 1f)
        ));
        addBar("reactorStatus", (RBMKConsoleBuild entity) -> new Bar(
            () -> "Status: " + entity.getStatusText(),
            () -> entity.getStatusColor(),
            () -> entity.getStatusProgress()
        ));
    }
    
    public class RBMKConsoleBuild extends RBMKBaseBuild{
        public float neutronFlux = 0f;
        public Seq<Building> reactorComponents = new Seq<>();
        public int totalRods = 0;
        public int activeRods = 0;
        public float averageHeat = 25f;
        public float maxHeat = 25f;
        public int controlRodCount = 0;
        public float totalPower = 0f;
        
        // 中子通量历史记录缓冲区
        public float[] fluxBuffer = new float[fluxDisplayBuffer];
        public int fluxBufferIndex = 0;
        
        // 结构俯视图数据
        public Seq<RBMKColumn> columns = new Seq<>();
        public static final int gridSize = 15; // 15x15网格
        
        // 原点坐标
        public int originX = -1;
        public int originY = -1;
        public int originZ = -1;
        
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 扫描周围的反应堆组件
            scanReactorComponents();
            
            // 计算中子通量
            calculateNeutronFlux();
            
            // 更新中子通量缓冲区
            updateFluxBuffer();
        }
        
        public void updateFluxBuffer(){
            // 移动缓冲区
            for(int i = 0; i < fluxBuffer.length - 1; i++){
                fluxBuffer[i] = fluxBuffer[i + 1];
            }
            fluxBuffer[fluxBuffer.length - 1] = neutronFlux;
        }
        
        public void scanReactorComponents(){
            reactorComponents.clear();
            totalRods = 0;
            activeRods = 0;
            controlRodCount = 0;
            float totalHeat = 0;
            int heatCount = 0;
            maxHeat = 25f;
            totalPower = 0f;
            
            // 初始化结构俯视图数据
            columns.clear();
            for(int i = 0; i < gridSize * gridSize; i++){
                columns.add((RBMKColumn)null);
            }
            
            // 扫描周围的方块
            float scanRange = 200f;
            for(int dx = - (int)scanRange; dx <= (int)scanRange; dx++){
                for(int dy = - (int)scanRange; dy <= (int)scanRange; dy++){
                    Building build = world.build(tile.x + dx, tile.y + dy);
                    if(build != null && build != this){
                        // 检查是否是RBMK组件
                        if(build.block instanceof RBMKBase){
                            // 黑名单：排除RBMKConsole和RBMKPusher
                            if(build.block instanceof RBMKConsole || build.block instanceof RBMKPusher){
                                continue;
                            }
                            reactorComponents.add(build);
                            
                            // 添加到结构俯视图
                            if(originX != -1 && originY != -1 && originZ != -1){
                                // 使用原点坐标，2坐标为一刻度
                                int gridX = (int)((build.x - originX) / (tilesize * 2)) + gridSize / 2;
                                int gridY = (int)((build.y - originY) / (tilesize * 2)) + gridSize / 2;
                                if(gridX >= 0 && gridX < gridSize && gridY >= 0 && gridY < gridSize){
                                    int index = gridY * gridSize + gridX;
                                    columns.set(index, new RBMKColumn(build));
                                }
                            } else {
                                // 使用控制台坐标，2坐标为一刻度
                                int gridX = (int)((build.x - x) / (tilesize * 2)) + gridSize / 2;
                                int gridY = (int)((build.y - y) / (tilesize * 2)) + gridSize / 2;
                                if(gridX >= 0 && gridX < gridSize && gridY >= 0 && gridY < gridSize){
                                    int index = gridY * gridSize + gridX;
                                    columns.set(index, new RBMKColumn(build));
                                }
                            }
                            
                            // 统计燃料棒
                            if(build.block instanceof RBMKRod){
                                totalRods++;
                                RBMKRodBuild rodBuild = (RBMKRodBuild)build;
                                if(rodBuild.heat > 50f){
                                    activeRods++;
                                }
                                // 计算总功率
                                if(build.power != null){
                                    totalPower += build.power.graph.getLastPowerProduced();
                                }
                            }
                            
                            // 统计控制棒
                            if(build.block instanceof RBMKControl){
                                controlRodCount++;
                            }
                            
                            // 计算平均温度和最大温度
                            RBMKBaseBuild baseBuild = (RBMKBaseBuild)build;
                            totalHeat += baseBuild.heat;
                            maxHeat = Math.max(maxHeat, baseBuild.heat);
                            heatCount++;
                        }
                    }
                }
            }
            
            // 计算平均温度
            if(heatCount > 0){
                averageHeat = totalHeat / heatCount;
            } else {
                // 没有找到任何反应堆组件，重置为默认温度
                averageHeat = 25f;
                maxHeat = 25f;
            }
        }
        
        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                return false;
            }
            
            // 选择原点
            if (other != null) {
                originX = (int)other.x;
                originY = (int)other.y;
                originZ = 0; // 简化为0，因为Tile没有z属性
                Core.app.post(() -> {
                    Dialog dialog = new Dialog("Origin Set");
                    dialog.add("Origin set to: (" + other.x + ", " + other.y + ", " + 0 + ")");
                    dialog.button("OK", dialog::hide);
                    dialog.show();
                });
                return false;
            }
            
            return true;
        }
        
        @Override
        public void drawConfigure(){
            super.drawConfigure();
            
            // 绘制原点标记
            if(originX != -1 && originY != -1){
                Draw.z(Layer.blockOver);
                Draw.color(Color.green);
                Lines.stroke(3f);
                Lines.square(originX, originY, 16f);
                Draw.color();
            }
        }
        
        public void calculateNeutronFlux(){            // 计算所有结构的中子通量平均值
            float totalFlux = 0f;
            int count = 0;
            
            for(Building build : reactorComponents){                if(build instanceof RBMKBaseBuild baseBuild){                    totalFlux += baseBuild.neutronFlux;
                    count++;
                }
            }
            
            if(count > 0){                neutronFlux = totalFlux / count;
            } else {
                neutronFlux = 0f;
            }
            
            neutronFlux = Mathf.clamp(neutronFlux, 0f, 10000f);
        }
        
        public String getStatusText(){            if(averageHeat < 100f) return "Cold";
            if(averageHeat < 500f) return "Stable";
            if(averageHeat < 800f) return "Hot";
            return "Critical";
        }
        
        public Color getStatusColor(){            if(averageHeat < 100f) return Color.blue;
            if(averageHeat < 500f) return Color.green;
            if(averageHeat < 800f) return Color.yellow;
            return Color.red;
        }
        
        public float getStatusProgress(){            return Mathf.clamp(averageHeat / 1000f, 0f, 1f);
        }
        
        @Override
        public void draw(){
            super.draw();

            // 绘制结构预览
            drawStructurePreview();
        }
        
        public void drawNeutronFlux(){
            Draw.z(Layer.blockOver);
            
            // 绘制中子通量图表 - 使用历史缓冲区
            float chartWidth = size * tilesize - 32f;
            float chartHeight = 64f;
            float chartY = y + size * tilesize / 2f - chartHeight / 2f - 16f;
            float chartX = x;
            
            // 绘制中子通量历史曲线
            if(fluxBuffer.length > 0){
                Draw.color(Color.yellow);
                Lines.stroke(2f);
                Lines.beginLine();
                
                for(int i = 0; i < fluxBuffer.length; i++){
                    float t = i / (float)(fluxBuffer.length - 1);
                    float xPos = chartX - chartWidth / 2f + chartWidth * t;
                    float fluxValue = fluxBuffer[i];
                    float yPos = chartY - chartHeight / 2f + (chartHeight * Mathf.clamp(fluxValue / 10000f, 0f, 1f));
                    Lines.linePoint(xPos, yPos);
                }
                
                Lines.endLine();
            }
            
            Draw.color();
        }
        
        public void drawStructurePreview(){
            Draw.z(Layer.blockOver);
            
            // 绘制结构俯视图背景（缩小1倍，居中显示）
            float previewSize = 60f;
            float previewX = x + size * tilesize / 2f - previewSize / 2f - 18f;
            float previewY = y + size * tilesize / 2f - previewSize / 2f - 18f;
            
            Draw.color(Color.darkGray);
            Fill.rect(previewX, previewY, previewSize, previewSize);
            
            // 绘制网格线（变细2倍）
            Draw.color(Color.gray);
            Lines.stroke(0.5f);
            float cellSize = previewSize / gridSize;
            for(int i = 0; i <= gridSize; i++){
                float linePos = previewY - previewSize / 2f + cellSize * i;
                Lines.line(previewX - previewSize / 2f, linePos, previewX + previewSize / 2f, linePos);
                linePos = previewX - previewSize / 2f + cellSize * i;
                Lines.line(linePos, previewY - previewSize / 2f, linePos, previewY + previewSize / 2f);
            }
            Lines.stroke(1f); // 恢复默认线条粗细
            
            // 绘制反应堆组件
            for(int i = 0; i < columns.size; i++){
                RBMKColumn col = columns.get(i);
                if(col != null){
                    int gridX = i % gridSize;
                    int gridY = i / gridSize;
                    
                    float cellX = previewX - previewSize / 2f + cellSize * gridX + cellSize / 2f;
                    float cellY = previewY - previewSize / 2f + cellSize * gridY + cellSize / 2f;
                    
                    // 根据组件类型设置颜色
                    Color colColor = col.getColor();
                    Draw.color(colColor);
                    Fill.rect(cellX, cellY, cellSize - 2, cellSize - 2);
                    
                    // 如果是控制棒，显示控制棒位置
                    if(col.build.block instanceof RBMKControl){
                        RBMKControl.RBMKControlBuild control = (RBMKControl.RBMKControlBuild)col.build;
                        
                        // 计算控制棒插入高度（从底部开始）
                        float rodHeight = cellSize * (1f - control.controlValue);
                        float rodY = cellY - (cellSize - 4) / 2f;
                        
                        // 绘制控制棒（深色条形显示插入深度）- 置于结构预览图上方
                        Draw.z(Layer.blockAdditive);
                        Draw.color(Color.black);
                        Fill.rect(cellX, rodY + rodHeight / 2f, cellSize - 4, rodHeight);
                        Draw.z(Layer.blockOver); // 恢复到原始图层
                        Draw.color();
                    }
                }
            }
            
            // 绘制控制台位置标记（缩小1倍）
            Draw.color(Color.white);
            float centerX = previewX;
            float centerY = previewY;
            Lines.stroke(1f);
            Lines.line(centerX - 2, centerY, centerX + 2, centerY);
            Lines.line(centerX, centerY - 2, centerX, centerY + 2);
            
            Draw.color();
        }
        
        
        @Override
        public void buildConfiguration(Table table){
            // 添加动画覆盖层
            Table animOverlay = new Table();
            animOverlay.background(Styles.black6);
            animOverlay.center();
            
            // 添加动画标签
            animOverlay.add("@").color(Color.yellow).pad(40).row();
            animOverlay.label(() -> "RBMK OS").color(Color.green).pad(50).row();
            
            table.add(animOverlay).grow().minHeight(400);
            
            // 0.75秒后移除动画效果并显示正常UI
            Time.run(45f, () -> {
                animOverlay.remove();
                showNormalUI(table);
            });
        }
        
        public void showNormalUI(Table table) {
            Table cont = new Table().top();
            cont.left().defaults().left().growX();
            
            cont.table(Styles.grayPanel, info -> {
                info.left().defaults().left();
                info.add("[accent]RBMK Control Console[]").row();
                info.image().color(Pal.accent).growX().height(2).row();
            }).growX().left().pad(10);
            cont.row();
            
            // 原点设置提示
            if(originX == -1 || originY == -1 || originZ == -1){
                cont.table(Styles.grayPanel, originInfo -> {
                    originInfo.left().defaults().left();
                    originInfo.add("[red]Origin not set![]").row();
                    originInfo.add("Click on a block to set it as the origin for the reactor structure view.");
                }).growX().left().pad(10);
                cont.row();
            } else {
                cont.table(Styles.grayPanel, originInfo -> {
                    originInfo.left().defaults().left();
                    originInfo.add("[green]Origin set to:[] " + originX + ", " + originY + ", " + originZ).row();
                    originInfo.add("Click on another block to change the origin.");
                }).growX().left().pad(10);
                cont.row();
            }
            
            // 结构俯视图
            cont.table(Styles.grayPanel, structure -> {
                structure.left().defaults().left();
                structure.add("[accent]Reactor Structure:[]").row();
                
                // 绘制结构俯视图
                structure.table(preview -> {
                    preview.left().defaults().left();
                    
                    // 创建结构预览画布
                    Table gridTable = new Table();
                    float cellSize = 16f;
                    
                    for(int y = gridSize - 1; y >= 0; y--){ 
                        Table row = new Table();
                        for(int x = 0; x < gridSize; x++){
                            int index = y * gridSize + x;
                            RBMKColumn col = columns.get(index);
                            
                            Table cellContainer = new Table();
                            
                            if(col != null){
                                // 左侧温度状态条 (2像素宽)
                                Table barContainer = new Table();
                                barContainer.background(Tex.whiteui);
                                barContainer.setColor(Color.darkGray);
                                
                                if(col.build instanceof RBMKBaseBuild){
                                    float maxTemperature = 1000f;
                                    
                                    barContainer.update(() -> {
                                        barContainer.clearChildren();
                                        float currentTemp = ((RBMKBaseBuild)col.build).heat;
                                        float barHeight = cellSize * Mathf.clamp(currentTemp / maxTemperature, 0f, 1f);
                                        
                                        // 绘制温度状态条（底部对齐）
                                        Table bar = new Table();
                                        bar.background(Tex.whiteui);
                                        bar.setColor(Color.red);
                                        barContainer.add(bar).size(2, (int)barHeight).pad(0).bottom().expand();
                                    });
                                }
                                cellContainer.add(barContainer).size(2, (int)cellSize).padRight(0);
                                
                                // 右侧结构预览图 (12像素宽，加上2像素温度条和2像素间隙，总宽度16像素，形成正方形)
                                Table previewContainer = new Table();
                                previewContainer.background(Tex.whiteui);
                                previewContainer.setColor(Color.darkGray);
                                
                                // 绘制组件方块
                                Color colColor = col.getColor();
                                Table cell = new Table();
                                cell.background(Tex.whiteui);
                                cell.setColor(colColor);
                                
                                // 如果是控制棒，显示控制棒位置
                                if(col.build.block instanceof RBMKControl){
                                    RBMKControl.RBMKControlBuild control = (RBMKControl.RBMKControlBuild)col.build;
                                    cell.update(() -> {
                                        float alpha = control.controlValue;
                                        cell.setColor(Tmp.c1.set(colColor).lerp(Color.black, 1f - alpha));
                                    });
                                }
                                // 结构预览图为12x12像素，加上1像素内边距，预览容器为14x16像素
                                // 总宽度：2(温度条) + 2(间隙) + 12(组件) = 16像素，与高度相同
                                previewContainer.add(cell).size(12, 12).pad(1);
                                
                                cellContainer.add(previewContainer).size(14, 16);
                            } else {
                                Table cell = new Table();
                                cell.background(Tex.whiteui);
                                cell.setColor(Color.darkGray);
                                cellContainer.add(cell).size((int)cellSize);
                            }
                            
                            row.add(cellContainer).pad(1);
                        }
                        gridTable.add(row).row();
                    }
                    
                    preview.add(gridTable);
                }).growX().left().pad(10);
            }).growX().left().pad(10);
            cont.row();
            
            // 中子通量图
            cont.table(Styles.grayPanel, flux -> {
                flux.left().defaults().left();
                flux.add("[accent]Neutron Flux History:[]").row();
                
                // 绘制中子通量历史图
                flux.table(chart -> {
                    chart.left().defaults().left();
                    
                    // 创建中子通量图表
                    Table chartTable = new Table();
                    float chartWidth = 300f;
                    float chartHeight = 100f;
                    
                    chartTable.background(Styles.black6);
                    
                    // 绘制中子通量曲线
                    chartTable.update(() -> {
                        chartTable.clearChildren();
                        
                        if(fluxBuffer.length > 0){
                            Table lineTable = new Table();
                            lineTable.left().bottom();
                            
                            for(int i = 0; i < fluxBuffer.length; i++){
                                float value = fluxBuffer[i];
                                float height = chartHeight * Mathf.clamp(value / 6000f, 0f, 1f);
                                
                                Table bar = new Table();
                                bar.background(Tex.whiteui);
                                bar.setColor(Color.yellow);
                                
                                lineTable.add(bar).size((int)(chartWidth / fluxBuffer.length - 1), (int)height).padRight(1);
                            }
                            
                            chartTable.add(lineTable).grow();
                        }
                    });
                    
                    chart.add(chartTable).width((int)chartWidth).height((int)chartHeight);
                }).growX().left().pad(10);
                
                flux.row();
                flux.add("Current Flux: ").color(Color.yellow);
                Label currentFluxLabel = new Label(() -> (int)neutronFlux + "");
                currentFluxLabel.setColor(Color.yellow);
                flux.add(currentFluxLabel).row();
            }).growX().left().pad(10);
            cont.row();
            
            // 状态概览
            cont.table(Styles.grayPanel, stats -> {
                stats.left().defaults().left();
                stats.add("[accent]Reactor Status:[]").row();
                
                stats.add("Components: " ).left();
                stats.add(new Label(() -> (reactorComponents.size / 4) + "")).row();
                
                stats.add("Fuel Rods: " ).left();
                stats.add(new Label(() -> (activeRods / 4) + "/" + (totalRods / 4))).row();
                
                stats.add("Control Rods: " ).left();
                stats.add(new Label(() -> controlRodCount + "")).row();
                
                stats.add("Avg Heat: " ).left();
                Label avgHeatLabel = new Label(() -> (int)averageHeat + "°C");
                avgHeatLabel.update(() -> {
                    if(averageHeat > 800) avgHeatLabel.setColor(Color.red);
                    else if(averageHeat > 500) avgHeatLabel.setColor(Color.orange);
                    else avgHeatLabel.setColor(Color.white);
                });
                stats.add(avgHeatLabel).row();
                
                stats.add("Max Heat: " ).left();
                Label maxHeatLabel = new Label(() -> (int)maxHeat + "°C");
                maxHeatLabel.update(() -> {
                    if(maxHeat > 800) maxHeatLabel.setColor(Color.red);
                    else if(maxHeat > 500) maxHeatLabel.setColor(Color.orange);
                    else maxHeatLabel.setColor(Color.white);
                });
                stats.add(maxHeatLabel).row();
                
                stats.add("Neutron Flux: " ).left();
                stats.add(new Label(() -> (int)neutronFlux + "")).row();
                
                stats.add("Power Output: " ).left();
                stats.add(new Label(() -> (int)(totalPower * 60) + " MW")).row();
            }).growX().left().pad(10);
            cont.row();
            
            // 控制棒调节
            cont.table(Styles.grayPanel, control -> {
                control.left().defaults().left();
                control.add("[accent]Control Rods:[]").row();
                
                Slider controlRodSlider = new Slider(0, 100, 1, false);
                controlRodSlider.setValue(100f); // 默认完全抽出
                
                // 计算当前平均控制棒位置
                float currentControlValue = 100f;
                if(controlRodCount > 0){
                    int total = 0;
                    float sum = 0f;
                    for(Building build : reactorComponents){
                        if(build instanceof RBMKControl.RBMKControlBuild controlBuild){
                            sum += controlBuild.targetControlValue * 100f;
                            total++;
                        }
                    }
                    if(total > 0){
                        currentControlValue = sum / total;
                    }
                }
                controlRodSlider.setValue(currentControlValue);
                
                // 实时更新滑块位置
                controlRodSlider.update(() -> {
                    if(controlRodCount > 0){
                        int total = 0;
                        float sum = 0f;
                        for(Building build : reactorComponents){
                            if(build instanceof RBMKControl.RBMKControlBuild controlBuild){
                                sum += controlBuild.targetControlValue * 100f;
                                total++;
                            }
                        }
                        if(total > 0){
                            float avgValue = sum / total;
                            if(Math.abs(controlRodSlider.getValue() - avgValue) > 1f){
                                controlRodSlider.setValue(avgValue);
                            }
                        }
                    }
                });
                
                controlRodSlider.changed(() -> {
                    float value = controlRodSlider.getValue() / 100f;
                    for(Building build : reactorComponents){
                        if(build instanceof RBMKControl.RBMKControlBuild controlBuild){
                            controlBuild.targetControlValue = value;
                        }
                    }
                });
                
                Table sliderTable = new Table();
                sliderTable.add("Withdrawal: " ).left();
                sliderTable.add(controlRodSlider).growX();
                sliderTable.add(new Label(() -> (int)controlRodSlider.getValue() + "%")).width(50);
                control.add(sliderTable).growX().row();
            }).growX().left().pad(10);
            cont.row();
            
            // 紧急控制
            cont.table(Styles.grayPanel, emergency -> {
                emergency.left().defaults().left();
                emergency.add("[accent]Emergency Controls:[]").row();
                
                Button az5Button = new Button(Styles.defaulti);
                az5Button.add("AZ-5 EMERGENCY SHUTDOWN");
                az5Button.clicked(() -> {
                    // 插入所有控制棒
                    for(Building build : reactorComponents){
                        if(build instanceof RBMKControl.RBMKControlBuild control){
                            // 直接设置到0%，不使用平滑过渡
                            control.controlValue = 0f;
                            control.targetControlValue = 0f;
                        }
                        if(build instanceof RBMKRodBuild rod){
                            rod.heat = Mathf.clamp(rod.heat - 100f, 25f, rod.maxHeat);
                        }
                    }
                    
                    Core.app.post(() -> {
                        Dialog dialog = new Dialog("AZ-5 ENGAGED");
                        dialog.add("Emergency shutdown initiated! All control rods inserted.");
                        dialog.button("OK", dialog::hide);
                        dialog.show();
                    });
                });
                emergency.add(az5Button).growX().row();
            }).growX().left().pad(10);
            cont.row();
            
            Table main = new Table().background(Styles.black6);
            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(false, false);
            pane.setOverscroll(false, false);
            
            Table scrollTable = new Table();
            scrollTable.add(pane).maxWidth(600).maxHeight(600);
            scrollTable.row();
            
            Slider horizontalSlider = new Slider(0, 100, 1, false);
            horizontalSlider.changed(() -> {
                float scrollPos = horizontalSlider.getValue() / 100f;
                pane.setScrollX(scrollPos * (pane.getMaxWidth() - pane.getWidth()));
            });
            scrollTable.add(horizontalSlider).width(600).height(20);
            
            main.add(scrollTable);
            table.top().add(main);
        }
        
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(neutronFlux);
            write.f(averageHeat);
            write.f(maxHeat);
            write.i(controlRodCount);
            write.f(totalPower);
            
            // 写入中子通量缓冲区
            for(float flux : fluxBuffer){
                write.f(flux);
            }
            
            // 写入原点坐标
            write.i(originX);
            write.i(originY);
            write.i(originZ);
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            neutronFlux = read.f();
            averageHeat = read.f();
            maxHeat = read.f();
            controlRodCount = read.i();
            totalPower = read.f();
            
            // 读取中子通量缓冲区
            if(revision >= 1){
                for(int i = 0; i < fluxBuffer.length; i++){
                    fluxBuffer[i] = read.f();
                }
                
                // 读取原点坐标
                if(revision >= 2){
                    originX = read.i();
                    originY = read.i();
                    originZ = read.i();
                }
            }
        }
        
        @Override
        public byte version(){
            return 2;
        }
    }
    
    // 结构俯视图列数据类
    public static class RBMKColumn{
        public Building build;
        
        public RBMKColumn(Building build){
            this.build = build;
        }
        
        public Color getColor(){
            if(build == null) return Color.clear;
            
            if(build.block instanceof RBMKRod){
                RBMKRodBuild rod = (RBMKRodBuild)build;
                if(rod.heat > 800) return Color.red;
                if(rod.heat > 500) return Color.orange;
                if(rod.heat > 200) return Color.yellow;
                return Color.scarlet;
            } else if(build.block instanceof RBMKControl){
                return Color.green;
            } else if(build.block instanceof RBMKCooler){
                return Color.cyan;
            } else if(build.block instanceof RBMKModerator){
                return Color.blue;
            } else if(build.block instanceof RBMKReflector){
                return Color.purple;
            } else if(build.block instanceof RBMKAbsorber){
                return Color.gray;
            } else if(build.block instanceof RBMKBoiler){
                return Color.sky;
            }
            
            return Color.white;
        }
    }
}
