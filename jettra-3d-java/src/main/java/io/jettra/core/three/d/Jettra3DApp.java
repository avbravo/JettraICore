package io.jettra.core.three.d;

import java.util.List;

import static com.raylib.Raylib.*;
import com.raylib.Color;
import com.raylib.Vector3;
import com.raylib.Vector2;
import com.raylib.Rectangle;
import com.raylib.Camera3D;

import io.jettra.core.three.d.model.Artifact;
import io.jettra.core.three.d.model.HumanEntity;
import io.jettra.core.three.d.model.MegaProject;
import io.jettra.core.three.d.model.Thought;
import io.jettra.core.three.d.model.WorldEvent;
import static com.raylib.Raylib.ConfigFlags.*;
import static com.raylib.Raylib.KeyboardKey.*;
import static com.raylib.Raylib.MouseButton.*;
import static com.raylib.Raylib.CameraProjection.*;
import static com.raylib.Raylib.CameraMode.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Jettra3DApp {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private Camera3D camera;
    private List<Artifact> artifacts = new CopyOnWriteArrayList<>();
    private List<HumanEntity> entities = new CopyOnWriteArrayList<>();
    private List<MegaProject> megaProjects = new CopyOnWriteArrayList<>();
    private List<Thought> thoughts = new CopyOnWriteArrayList<>();
    private List<WorldEvent> worldEvents = new CopyOnWriteArrayList<>();

    private float worldTime = 0;
    private int selectedAgentIndex = -1;
    private int selectedArtifactIndex = -1;
    private boolean followMode = false;
    private boolean directorMode = false;
    private float directorTimer = 0;
    private float planeScale = 1.0f;
    private boolean isAnchored = false;
    private boolean cameraLocked = false;
    private boolean showHelp = true;
    private boolean showConfigModal = false;
    private boolean voiceEnabled = true;

    // Config Toggles
    private boolean configEnabledApps = true;
    private boolean configEnabledInternet = true;
    private boolean configEnabledFiles = true;
    private boolean configEnabledLife = true;
    private boolean configEnabledSocial = true;
    
    // Chat & Knowledge UI
    private boolean showChat = false;
    private String chatInput = "";
    private List<String> chatHistory = new java.util.ArrayList<>();
    private List<KnowledgeEntry> topKnowledge = new java.util.ArrayList<>();
    private float topKnowledgeUpdateTimer = 0;

    // Environmental
    private float timeScale = 1.0f;
    private int weatherMode = 0; // 0: Sunny, 1: Night, 2: Storm
    private boolean sfxEnabled = true;
    private float howlTimer = 0;

    public void run() {
        setConfigFlags(FLAG_WINDOW_RESIZABLE);
        initWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Jettra 3D Core - Java 25 Native");
        maximizeWindow();
        setTargetFPS(60);

        initAudioDevice();
        initSfx();

        initCamera();
        initPopulation();

        while (!windowShouldClose()) {
            float dt = getFrameTime();
            float scaledDt = dt * timeScale;
            worldTime += scaledDt;

            handleInput();
            update(scaledDt);
            draw();
        }

        closeAudioDevice();
        closeWindow();
    }

    private void initCamera() {
        camera = new Camera3D();
        camera.position(new Vector3().x(35).y(35).z(35));
        camera.target(new Vector3().x(0).y(0).z(0));
        camera.up(new Vector3().x(0).y(1).z(0));
        camera.fovy(45);
        camera.projection(CAMERA_PERSPECTIVE);
    }

    private void handleInput() {
        if (isKeyPressed(KEY_TAB)) showHelp = !showHelp;
        if (isKeyPressed(KEY_C)) {
            initCamera();
            followMode = false;
            cameraLocked = false;
        }
        if (isKeyPressed(KEY_F)) {
            followMode = !followMode;
            if (followMode && selectedAgentIndex == -1 && !entities.isEmpty()) {
                selectedAgentIndex = 0;
            }
        }
        if (isKeyPressed(KEY_L)) isAnchored = !isAnchored;
        
        // Chat Toggle
        if (isKeyPressed(KEY_ENTER)) {
            if (showChat && !chatInput.trim().isEmpty()) {
                sendChatMessage(chatInput);
                chatInput = "";
            } else {
                showChat = !showChat;
            }
        }

        if (showChat) {
            int key = getCharPressed();
            while (key > 0) {
                if ((key >= 32) && (key <= 125)) {
                    chatInput += (char)key;
                }
                key = getCharPressed();
            }
            if (isKeyPressed(KEY_BACKSPACE) && !chatInput.isEmpty()) {
                chatInput = chatInput.substring(0, chatInput.length() - 1);
            }
        }

        float wheel = getMouseWheelMove();
        if (wheel != 0) {
            camera.fovy(camera.fovy() - wheel * 2);
            if (camera.fovy() < 5) camera.fovy(5);
            if (camera.fovy() > 120) camera.fovy(120);
        }
    }

    private void update(float dt) {
        if (followMode && selectedAgentIndex != -1 && selectedAgentIndex < entities.size()) {
            HumanEntity e = entities.get(selectedAgentIndex);
            camera.target(new Vector3().x(e.x).y(e.y).z(e.z));
            camera.position(new Vector3().x(e.x + 10).y(e.y + 10).z(e.z + 10));
        } else if (!cameraLocked && !isAnchored) {
            updateCamera(camera, CAMERA_FREE);
        }

        updateEntities(dt);
        updateThoughts(dt);
        
        topKnowledgeUpdateTimer += dt;
        if (topKnowledgeUpdateTimer > 2.0f) {
            updateTopKnowledge();
            topKnowledgeUpdateTimer = 0;
        }

        if (directorMode) {
            updateDirectorMode(dt);
        }

        updateSfx();
        
        if (howlTimer > 0) howlTimer -= dt;
        checkPackSafety(dt);
    }

    private void checkPackSafety(float dt) {
        if (weatherMode == 2 && howlTimer <= 0) {
            long total = 0;
            long sheltered = 0;
            HumanEntity jettra = null;
            
            for (HumanEntity e : entities) {
                if (e.name.contains("Jettra")) { jettra = e; continue; }
                if (e.isCar || e.isAnimal) continue;
                total++;
                if ("SHELTERED".equals(e.action)) sheltered++;
            }
            
            if (total > 0 && sheltered == total && jettra != null) {
                // Reward agents for successful survival tactics
                for (HumanEntity re : entities) {
                    if ("SHELTERED".equals(re.action) && !re.name.contains("Jettra")) {
                        re.intelligence += 2;
                        re.currentThought = "He aprendido a sobrevivir (+2 Intel)";
                        re.thoughtTimer = 4.0f;
                        final HumanEntity fRe = re;
                        thoughts.add(new Thought() {{
                            content = "LEARNED: SURVIVAL (+2 Intel)";
                            x = fRe.x; y = fRe.y + 3.0f; z = fRe.z;
                            timer = 4.0f; isThought = false;
                        }});
                    }
                }

                final String msg = "¡AWOOOOOOOOOO! La manada está a salvo de la tormenta.";
                final HumanEntity fJettra = jettra;
                jettra.currentThought = msg;
                jettra.thoughtTimer = 6.0f;
                thoughts.add(new Thought() {{
                    content = msg; x = fJettra.x; y = fJettra.y + 4.0f; z = fJettra.z;
                    timer = 6.0f; isThought = false;
                }});
                worldEvents.add(new WorldEvent("Aullido de Jettra: Supervivencia Exitosa", worldTime, 255, 255, 0));
                howlTimer = 40.0f; // Relax for 40s
            }
        }
    }

    private void updateEntities(float dt) {
        for (HumanEntity e : entities) {
            // --- WEATHER & TIME REACTION ---
            float envSpeed = 1.0f;
            if (weatherMode == 1) envSpeed = 0.4f; // Night: very slow
            if (weatherMode == 2) { 
                envSpeed = 0.7f; // Storm: somewhat slow
                // Seeking Shelter logic
                if (worldTime % 2.0 < dt && !artifacts.isEmpty() && !e.isCar && !e.isWolf) {
                    Artifact nearest = artifacts.get(0);
                    float minDistSq = Float.MAX_VALUE;
                    for (Artifact a : artifacts) {
                        float d = (a.x - e.x)*(a.x - e.x) + (a.z - e.z)*(a.z - e.z);
                        if (d < minDistSq) { minDistSq = d; nearest = a; }
                    }
                    if (minDistSq > 9.0f) { // Only if > 3 units away
                        e.targetX = nearest.x; e.targetZ = nearest.z;
                        if (Math.random() < 0.1 && e.thoughtTimer <= 0) {
                            e.currentThought = "¡Está lloviendo! Al refugio...";
                            e.thoughtTimer = 3.0f;
                        }
                    } else {
                        e.action = "SHELTERED";
                    }
                }
            }

            // Movement logic
            float dx = e.targetX - e.x;
            float dz = e.targetZ - e.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);
            
            if (dist > 0.1f) {
                float baseSpeed = e.isCar ? 10.0f : 2.0f;
                float finalSpeed = baseSpeed * envSpeed;
                e.x += (dx / dist) * finalSpeed * dt;
                e.z += (dz / dist) * finalSpeed * dt;
                e.rotation = (float)Math.atan2(dx, dz) * (180.0f / (float)Math.PI);
            } else {
                // Reach target, pick new one
                float idleChance = (weatherMode == 1) ? 0.002f : 0.01f; // Night agents stay idle longer
                if (Math.random() < idleChance) {
                    e.targetX = (float)(Math.random()*80-40);
                    e.targetZ = (float)(Math.random()*80-40);
                    if (!e.isCar && !e.isWolf && Math.random() < 0.3) {
                        e.currentThought = "Voy a " + (Math.random() > 0.5 ? "explorar" : "descansar");
                        e.thoughtTimer = 3.0f;
                        thoughts.add(new Thought() {{
                            content = e.currentThought;
                            x = e.x; y = e.y + 3; z = e.z;
                            timer = 3.0f;
                            isThought = true;
                        }});
                    }
                }
            }

            // --- LEADERSHIP & AUTONOMOUS BEHAVIORS ---
            boolean isJettra = e.name.contains("Jettra");

            // 1. Socializing / Leadership Dialogues
            if (!e.isCar && worldTime % 3.0 < dt) { 
                for (HumanEntity target : entities) {
                    if (target == e) continue;
                    float tx = target.x - e.x;
                    float tz = target.z - e.z;
                    float distSqr = tx*tx + tz*tz;
                    
                    if (distSqr < 49.0f) { // Leadership range (7 units)
                        if (isJettra && Math.random() < 0.2) {
                            String cmd = (Math.random() > 0.5) ? "¡Agentes, a construir!" : "¡Sigamos adelante!";
                            e.action = "COMMANDING";
                            thoughts.add(new Thought() {{
                                content = cmd; x = e.x; y = e.y + 3.0f; z = e.z;
                                timer = 5.0f; isThought = false;
                            }});
                            // Influence nearby agent
                            if (Math.random() > 0.5) {
                                target.action = "BUILDING";
                                target.intelligence += 1;
                            }
                            break;
                        } else if (distSqr < 16.0f && Math.random() < 0.05) {
                            e.action = "SOCIALIZING";
                            String msg = e.isWolf ? "Awoooo!" : "¡Hola " + target.name + "!";
                            thoughts.add(new Thought() {{
                                content = msg; x = e.x; y = e.y + 2.5f; z = e.z;
                                timer = 4.0f; isThought = false;
                            }});
                            break;
                        }
                    }
                }
            }

            // 2. Building / Divine Creation (Jettra only)
            if (isJettra && Math.random() < 0.0005) {
                // Leader Jettra creates a finished Artifact instantly!
                final int finalType = (int)(Math.random()*4);
                artifacts.add(new Artifact() {{ 
                    x = e.x + 3; y = 0; z = e.z + 3; 
                    type = finalType;
                    r=255; g=215; b=0; a=255; // Gold color for Jettra's creations
                }});
                e.action = "CREATING";
                worldEvents.add(new WorldEvent("Jettra creó un objeto místico en ("+e.x+","+e.z+")", worldTime, 255, 215, 0));
            }

            // --- LEARNING SYSTEM (Memory & Web) ---
            if (!e.isCar && worldTime % 5.0 < dt) {
                double rand = Math.random();
                if (rand < 0.02) { // Simulate Web Search
                    String[] sources = {"Google", "ScienceDaily", "Medium Blog", "Digital Library"};
                    String source = sources[(int)(Math.random()*sources.length)];
                    String topic = (Math.random() > 0.5) ? "Mecánica Cuántica" : "Redes Neuronales";
                    e.currentThought = "Buscando " + topic + " en " + source + "...";
                    e.thoughtTimer = 4.0f;
                    saveKnowledge(e, source, topic);
                    worldEvents.add(new WorldEvent(e.name + " aprendió de " + source, worldTime, 100, 200, 255));
                } else if (rand < 0.04) { // Simulate Local Disk Scan
                    e.currentThought = "Escaneando archivos locales (PDF/MD)...";
                    e.thoughtTimer = 4.0f;
                    worldEvents.add(new WorldEvent(e.name + " analizando disco local", worldTime, 150, 150, 150));
                    saveKnowledge(e, "Disco Local", "Arquitectura del Sistema Jettra");
                }
            }

            // Normal building logic for others
            if (!e.isCar && !isJettra) {
                for (MegaProject p : megaProjects) {
                    float px = p.x - e.x;
                    float pz = p.z - e.z;
                    if (px*px + pz*pz < 25.0f) { 
                        p.progress += (e.action.equals("BUILDING") ? 0.005f : 0.001f) * dt;
                        e.action = "BUILDING";
                        if (p.progress >= 1.0f && !p.finished) {
                            p.finished = true;
                            worldEvents.add(new WorldEvent("Construcción completada por " + e.name, worldTime, 0, 255, 100));
                            artifacts.add(new Artifact() {{ x = p.x; y = p.y; z = p.z; type = 0; r=100; g=100; b=150; a=255; }});
                        }
                    }
                }
                
                // Spontaneous projects
                if (entities.size() > 5 && Math.random() < 0.0001) {
                    megaProjects.add(new MegaProject() {{ 
                        x = e.x + (float)Math.random()*10 - 5; y = 0; z = e.z + (float)Math.random()*10 - 5; progress = 0.01f; finished = false; 
                    }});
                }
            }
        }
    }

    private void updateThoughts(float dt) {
        entities.removeIf(e -> {
            if (e.thoughtTimer > 0) {
                e.thoughtTimer -= dt;
            }
            return false;
        });

        thoughts.removeIf(t -> {
            t.timer -= dt;
            return t.timer <= 0;
        });
    }

    private void updateDirectorMode(float dt) {
        directorTimer += dt;
        if (directorTimer > 8.0f || selectedAgentIndex == -1) {
            directorTimer = 0;
            for (int i = 0; i < entities.size(); i++) {
                HumanEntity e = entities.get(i);
                if ("Building".equals(e.action) || "Socializing".equals(e.action)) {
                    selectedAgentIndex = i;
                    followMode = true;
                    break;
                }
            }
        }
    }

    private void draw() {
        beginDrawing();
        
        Color skyColor = switch(weatherMode) {
            case 1 -> new Color().r((byte)2).g((byte)2).b((byte)5).a((byte)255); // Night
            case 2 -> new Color().r((byte)30).g((byte)35).b((byte)45).a((byte)255); // Storm
            default -> new Color().r((byte)5).g((byte)5).b((byte)12).a((byte)255); // Default
        };
        clearBackground(skyColor);

        beginMode3D(camera);
        drawGrid((int)(50 * planeScale), 1.0f);
        drawPlane(new Vector3().x(0).y(-0.05f).z(0), new Vector2().x(100 * planeScale).y(100 * planeScale), 
                  weatherMode == 2 ? DARKGRAY : new Color().r((byte)15).g((byte)15).b((byte)30).a((byte)255));

        drawArtifacts();
        drawEntities();
        drawThoughts();

        if (weatherMode == 2) { // Draw Rain
            for(int j=0; j<100; j++) {
                float rx = (float)(Math.random()*80-40);
                float rz = (float)(Math.random()*80-40);
                float ry = (float)(Math.random()*20);
                drawLine3D(new Vector3().x(rx).y(ry).z(rz), new Vector3().x(rx).y(ry-0.5f).z(rz), SKYBLUE);
            }
        }

        endMode3D();

        drawUI();

        if (showHelp) drawHelpOverlay();
        if (showConfigModal) drawConfigModal();
        if (showChat) drawChatWindow();
        
        drawTopKnowledgePanel();

        endDrawing();
    }

    private void drawArtifacts() {
        for (Artifact a : artifacts) {
            Vector3 pos = new Vector3().x(a.x).y(a.y).z(a.z);
            Color color = new Color().r((byte)a.r).g((byte)a.g).b((byte)a.b).a((byte)a.a);
            
            switch (a.type) {
                case 0 -> { // House
                    drawCube(pos, 2, 2, 2, color);
                    drawCubeWires(pos, 2, 2, 2, BLACK);
                    drawCylinder(new Vector3().x(pos.x()).y(pos.y() + 1).z(pos.z()), 0, 1.5f, 2, 4, MAROON);
                }
                case 1 -> { // School
                    drawCube(pos, 4, 3, 4, color);
                    drawCubeWires(pos, 4, 3, 4, BLACK);
                    drawCube(new Vector3().x(pos.x()).y(pos.y() + 2).z(pos.z()), 1, 2, 1, DARKGRAY);
                }
                case 2 -> { // City Center
                    drawCube(pos, 5, 2, 5, color);
                    drawSphere(new Vector3().x(pos.x()).y(pos.y() + 2).z(pos.z()), 1.5f, GOLD);
                }
                case 3 -> { // Radio
                    drawCylinder(pos, 0.5f, 0.5f, 5, 8, DARKGRAY);
                    if (( (int)(worldTime * 2) % 2) == 0) {
                        drawSphere(new Vector3().x(pos.x()).y(pos.y() + 5).z(pos.z()), 0.3f, RED);
                    }
                }
            }
        }
        
        for (MegaProject p : megaProjects) {
            if (p.finished) continue;
            Vector3 pos = new Vector3().x(p.x).y(p.y).z(p.z);
            drawCubeWires(pos, 2.1f, 2.1f, 2.1f, YELLOW);
            drawCube(pos, 2.0f, 2.0f * p.progress, 2.0f, fade(SKYBLUE, 0.5f));
        }
    }

    private void drawEntities() {
        int i = 0;
        for (HumanEntity e : entities) {
            Vector3 pos = new Vector3().x(e.x).y(e.y).z(e.z);
            Color color = new Color().r((byte)e.r).g((byte)e.g).b((byte)e.b).a((byte)e.a);

            if (e.isWolf) {
                drawWolf(pos, e.rotation, color);
            } else if (e.isCar) {
                drawCube(pos, 3.0f, 1.0f, 1.5f, color);
                drawCubeWires(pos, 3.0f, 1.0f, 1.5f, BLACK);
            } else {
                // Human Shape
                drawCapsule(new Vector3().x(pos.x()).y(pos.y()).z(pos.z()), 
                           new Vector3().x(pos.x()).y(pos.y() + 1.8f).z(pos.z()), 0.4f, 8, 8, color);
                drawSphere(new Vector3().x(pos.x()).y(pos.y() + 2.0f).z(pos.z()), 0.5f, color);
            }

            if (selectedAgentIndex == i) {
                drawCircle3D(new Vector3().x(pos.x()).y(pos.y() + 0.01f).z(pos.z()), 1.5f, new Vector3().x(1).y(0).z(0), 90, LIME);
            }

            // Name Tag
            Vector2 screenPos = getWorldToScreen(new Vector3().x(pos.x()).y(pos.y() + 2.5f).z(pos.z()), camera);
            drawText(e.name, (int)screenPos.x() - measureText(e.name, 10)/2, (int)screenPos.y(), 10, RAYWHITE);
            i++;
        }
    }

    private void drawThoughts() {
        for (Thought t : thoughts) {
            Vector2 screenPos = getWorldToScreen(new Vector3().x(t.x).y(t.y).z(t.z), camera);
            if (screenPos.x() > 0 && screenPos.x() < getScreenWidth() && screenPos.y() > 0 && screenPos.y() < getScreenHeight()) {
                String label = t.isThought ? "[Pensando...]" : "[Hablando]";
                int fontSize = 16;
                int textW = measureText(t.content, fontSize);
                
                float alpha = Math.min(1.0f, t.timer); // Fade out
                Color bubbleColor = t.isThought ? fade(PURPLE, 0.7f * alpha) : fade(DARKBLUE, 0.8f * alpha);
                Color textColor = fade(RAYWHITE, alpha);
                Color labelColor = fade(GOLD, alpha);

                // Bubble popup background
                drawRectangleRounded(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, bubbleColor);
                drawRectangleRoundedLines(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, fade(RAYWHITE, 0.3f * alpha));
                
                // Triangle pointer
                drawTriangle(new Vector2().x(screenPos.x()).y(screenPos.y() - 15),
                             new Vector2().x(screenPos.x() - 10).y(screenPos.y() - 25),
                             new Vector2().x(screenPos.x() + 10).y(screenPos.y() - 25), bubbleColor);

                drawText(label, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 55, 10, labelColor);
                drawText(t.content, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 40, fontSize, textColor);
            }
        }
    }

    private void drawUI() {
        int sw = getScreenWidth();
        int sh = getScreenHeight();

        // Right Sidebar
        drawRectangle(sw - 200, 0, 200, sh, fade(DARKGRAY, 0.9f));
        drawLine(sw - 200, 0, sw - 200, sh, RAYWHITE);

        drawText("JETTRA CORE", sw - 190, 20, 20, GOLD);
        drawText("JAVA 25 EDITION", sw - 190, 45, 10, SKYBLUE);

        // Buttons
        if (guiButton(sw - 190, 80, 180, 30, "RESET WORLD", RED)) {
            entities.clear();
            artifacts.clear();
            megaProjects.clear();
            worldEvents.add(new WorldEvent("Mundo reiniciado", worldTime, 255, 0, 0));
        }

        if (guiButton(sw - 190, 120, 180, 30, "SAVE STATE", LIME)) {
            // saveWorldState();
            worldEvents.add(new WorldEvent("Estado guardado", worldTime, 0, 255, 0));
        }

        if (guiButton(sw - 190, 160, 180, 30, "CONFIGURACIÓN", BLUE)) {
            showConfigModal = !showConfigModal;
        }

        if (guiButton(sw - 190, 200, 180, 30, directorMode ? "DIRECTOR: ON" : "DIRECTOR: OFF", directorMode ? ORANGE : GRAY)) {
            directorMode = !directorMode;
            if (directorMode) followMode = true;
        }

        // --- NEW BUTTONS ---
        if (guiButton(sw - 190, 240, 180, 25, isAnchored ? "PLANE: LOCKED" : "PLANE: UNLOCKED", isAnchored ? RED : GRAY)) {
            isAnchored = !isAnchored;
        }

        if (guiButton(sw - 190, 270, 180, 25, "CENTER MAP", DARKGRAY)) {
            initCamera();
            followMode = false;
            cameraLocked = false;
        }

        if (guiButton(sw - 190, 300, 85, 25, "ZOOM +", GRAY)) {
            camera.fovy(Math.max(5, camera.fovy() - 5));
        }

        if (guiButton(sw - 100, 300, 85, 25, "ZOOM -", GRAY)) {
            camera.fovy(Math.min(120, camera.fovy() + 5));
        }

        if (guiButton(sw - 190, 330, 180, 25, voiceEnabled ? "VOICE: ON" : "VOICE: OFF", voiceEnabled ? LIME : RED)) {
            voiceEnabled = !voiceEnabled;
            worldEvents.add(new WorldEvent("Voz " + (voiceEnabled ? "activada" : "desactivada"), worldTime, 200, 200, 0));
        }

        if (guiButton(sw - 190, 365, 85, 30, sfxEnabled ? "SFX: ON" : "SFX: OFF", sfxEnabled ? LIME : RED)) {
            sfxEnabled = !sfxEnabled;
            worldEvents.add(new WorldEvent("Efectos " + (sfxEnabled ? "activados" : "desactivados"), worldTime, 100, 255, 100));
        }

        if (guiButton(sw - 100, 365, 85, 30, "SALIR", DARKGRAY)) {
            closeWindow();
            System.exit(0);
        }

        // Selected Info
        int infoY = 410;
        if (selectedAgentIndex != -1 && selectedAgentIndex < entities.size()) {
            HumanEntity e = entities.get(selectedAgentIndex);
            drawText("AGENT: " + e.name, sw - 190, infoY, 15, RAYWHITE);
            drawText("ACTION: " + e.action, sw - 190, infoY + 20, 10, LIGHTGRAY);
            drawText("ENERGY: " + (int)e.energy + "%", sw - 190, infoY + 35, 10, fade(GREEN, 0.8f));
            drawText("INTEL: " + (int)e.intelligence, sw - 190, infoY + 50, 10, SKYBLUE);
        }

        // Live Feed
        drawLiveFeed(sw, sh);
        // History Log
        drawHistoryLog(sw, sh);
    }

    private boolean guiButton(int x, int y, int w, int h, String text, Color color) {
        Vector2 mouse = getMousePosition();
        boolean hover = checkCollisionPointRec(mouse, new Rectangle().x(x).y(y).width(w).height(h));
        
        drawRectangle(x, y, w, h, hover ? fade(color, 0.8f) : fade(color, 0.5f));
        drawRectangleLines(x, y, w, h, RAYWHITE);
        
        int fontSize = 15;
        int tw = measureText(text, fontSize);
        drawText(text, x + (w - tw)/2, y + (h - fontSize)/2, fontSize, RAYWHITE);
        
        return hover && isMouseButtonPressed(MOUSE_BUTTON_LEFT);
    }

    private void drawLiveFeed(int sw, int sh) {
        int fy = sh - 150;
        drawRectangle(sw - 350, fy, 140, 140, fade(BLACK, 0.6f));
        drawText("LIVE FEED", sw - 340, fy + 5, 12, GOLD);
        int j = 0;
        for (int i = worldEvents.size() - 1; i >= 0 && j < 8; i--, j++) {
            WorldEvent ev = worldEvents.get(i);
            drawText("> " + ev.message, sw - 340, fy + 25 + (j * 14), 10, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawHistoryLog(int sw, int sh) {
        int w = 350; int h = 200;
        int x = 10; int y = sh - h - 10;
        drawRectangle(x, y, w, h, fade(BLACK, 0.5f));
        drawRectangleLines(x, y, w, h, fade(SKYBLUE, 0.5f));
        drawText("LOG DE EVENTOS DEL SISTEMA", x + 10, y + 10, 12, GOLD);
        
        int j = 0;
        int maxLines = 12;
        for (int i = worldEvents.size() - 1; i >= 0 && j < maxLines; i--, j++) {
            WorldEvent ev = worldEvents.get(i);
            String timeStr = String.format("%.1fs", ev.timestamp);
            drawText("[" + timeStr + "] " + ev.message, x + 15, y + 30 + (j * 14), 10, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawHelpOverlay() {
        drawRectangle(10, 10, 260, 180, fade(BLACK, 0.6f));
        drawRectangleLines(10, 10, 260, 180, fade(GOLD, 0.5f));
        drawText("CONTROLES DE CÁMARA", 20, 20, 12, GOLD);
        drawText("- Teclas: W,S,A,D,Q,E", 25, 40, 10, RAYWHITE);
        drawText("- Mouse: Click Derecho Girar", 25, 55, 10, RAYWHITE);
        drawText("- Rueda Mouse: Zoom +/-", 25, 70, 10, RAYWHITE);
        drawText("- C: Cambiar Cámara / Reset", 25, 85, 10, RAYWHITE);
        drawText("- F: Modo Seguir Agente", 25, 100, 10, RAYWHITE);
        drawText("- L: Lock/Unlock Plano", 25, 115, 10, RAYWHITE);
        drawText("- Esc: Cerrar App", 25, 130, 10, RAYWHITE);
        drawText("- Tab: Toggle esta Ayuda", 25, 145, 10, RAYWHITE);
    }

    private void drawConfigModal() {
        int sw = getScreenWidth();
        int sh = getScreenHeight();
        drawRectangle(0, 0, sw, sh, fade(BLACK, 0.4f));
        
        int mw = 400; int mh = 350;
        int mx = (sw - mw) / 2;
        int my = (sh - mh) / 2;
        
        drawRectangle(mx, my, mw, mh, DARKGRAY);
        drawRectangleLines(mx, my, mw, mh, RAYWHITE);
        drawText("CONFIGURACIÓN DE AUTONOMÍA", mx + 20, my + 20, 20, GOLD);

        configEnabledApps = guiToggle(mx + 20, my + 60, "Auto-abrir Aplicaciones (VLC/Chrome)", configEnabledApps);
        configEnabledInternet = guiToggle(mx + 20, my + 90, "Búsqueda de Aprendizaje en Internet", configEnabledInternet);
        configEnabledFiles = guiToggle(mx + 20, my + 120, "Estudio de Archivos Locales (PDF/MD)", configEnabledFiles);
        configEnabledLife = guiToggle(mx + 20, my + 150, "Población Silvestre (Animales/Autos)", configEnabledLife);
        configEnabledSocial = guiToggle(mx + 20, my + 180, "Simulación Social Avanzada", configEnabledSocial);

        if (guiButton(mx + mw - 120, my + mh - 50, 100, 30, "CERRAR", GRAY)) {
            showConfigModal = false;
        }
    }

    private boolean guiToggle(int x, int y, String text, boolean value) {
        if (guiButton(x, y, 20, 20, value ? "X" : " ", value ? LIME : GRAY)) {
            value = !value;
        }
        drawText(text, x + 30, y + 5, 12, RAYWHITE);
        return value;
    }

    private void initPopulation() {
        // Create initial agents
        generateEntity("Jettra Wolf", 0, 0, 0, true, false, false); // El lobo Jettra como líder
        generateEntity("Wolf-Prime", 10, 0, 10, true, false, false);
        generateEntity("Human-Alpha", 15, 0, 0, false, false, false);
        generateEntity("Civic-01", -15, 0, -15, false, false, true);
        
        for (int i = 0; i < 6; i++) {
            generateEntity("Agent-" + (10 + i), (float)(Math.random()*60-30), 0, (float)(Math.random()*60-30), false, false, false);
        }
        worldEvents.add(new WorldEvent("Sistema Iniciado. Población generada.", worldTime, 200, 200, 255));
    }

    private void drawWolf(Vector3 pos, float rotDeg, Color color) {
        // Wolf Model - Native Shapes
        float x = pos.x(); float y = pos.y(); float z = pos.z();
        float rotRad = rotDeg * ( (float)Math.PI / 180.0f);
        
        // Body (Capsule/Cylinder)
        drawCapsule(new Vector3().x(x).y(y + 0.5f).z(z), 
                   new Vector3().x(x + (float)Math.sin(rotRad)*1.2f).y(y + 0.6f).z(z + (float)Math.cos(rotRad)*1.2f), 
                   0.5f, 8, 8, DARKGRAY);

        // Legs (4 cylinders)
        float legOff = 0.3f;
        drawCylinder(new Vector3().x(x - legOff).y(y).z(z - legOff), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x + legOff).y(y).z(z - legOff), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x - legOff + (float)Math.sin(rotRad)).y(y).z(z - legOff + (float)Math.cos(rotRad)), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);
        drawCylinder(new Vector3().x(x + legOff + (float)Math.sin(rotRad)).y(y).z(z + legOff + (float)Math.cos(rotRad)), 0.1f, 0.1f, 0.6f, 6, DARKGRAY);

        // Head (Sphere + Snout)
        float headX = x + (float)Math.sin(rotRad)*1.6f;
        float headZ = z + (float)Math.cos(rotRad)*1.6f;
        drawSphere(new Vector3().x(headX).y(y + 1.2f).z(headZ), 0.45f, DARKGRAY);
        drawCylinder(new Vector3().x(headX + (float)Math.sin(rotRad)*0.3f).y(y + 1.1f).z(headZ + (float)Math.cos(rotRad)*0.3f), 
                     0.2f, 0.1f, 0.4f, 6, BLACK); // Snout
        
        // Ears (Triangles represent ears)
        drawSphere(new Vector3().x(headX).y(y + 1.6f).z(headZ + 0.15f), 0.15f, LIGHTGRAY); // Ear L
        drawSphere(new Vector3().x(headX).y(y + 1.6f).z(headZ - 0.15f), 0.15f, LIGHTGRAY); // Ear R

        // Tail
        drawCapsule(new Vector3().x(x).y(y + 0.6f).z(z), 
                   new Vector3().x(x - (float)Math.sin(rotRad)*0.8f).y(y + 0.8f).z(z - (float)Math.cos(rotRad)*0.8f), 
                   0.15f, 4, 4, DARKGRAY);
    }

    private void generateEntity(String name, float x, float y, float z, boolean isWolf, boolean isAnimal, boolean isCar) {
        HumanEntity e = new HumanEntity();
        e.name = name;
        e.x = x; e.y = y; e.z = z;
        e.targetX = x; e.targetY = y; e.targetZ = z;
        e.isWolf = isWolf; e.isAnimal = isAnimal; e.isCar = isCar;
        e.energy = 100;
        e.action = "IDLE";
        e.r = (int)(Math.random()*255); e.g = (int)(Math.random()*255); e.b = (int)(Math.random()*255);
        entities.add(e);
    }

    private static class KnowledgeEntry {
        public long timestamp;
        public String agent;
        public String source;
        public String topic;
        public float x, y, z;
        public double confidence;
        public String context;
        public String[] keywords;
    }

    private com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private void saveKnowledge(HumanEntity agent, String source, String topic) {
        try {
            java.io.File dir = new java.io.File("memory/data");
            if (!dir.exists()) dir.mkdirs();
            
            KnowledgeEntry entry = new KnowledgeEntry();
            entry.timestamp = System.currentTimeMillis();
            entry.agent = agent.name;
            entry.source = source;
            entry.topic = topic;
            entry.x = agent.x; entry.y = agent.y; entry.z = agent.z;
            entry.confidence = 0.7 + (Math.random() * 0.3); // High confidence
            entry.context = agent.action;
            entry.keywords = new String[]{topic.toLowerCase().replace(" ", "-"), "jettra-learning", source.toLowerCase()};

            java.io.File file = new java.io.File("memory/data/knowledge.json");
            java.util.List<KnowledgeEntry> list;
            if (file.exists()) {
                list = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<KnowledgeEntry>>() {});
            } else {
                list = new java.util.ArrayList<>();
            }

            // --- KNOWLEDGE FUSION LOGIC ---
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                KnowledgeEntry existing = list.get(i);
                if (existing.topic.equalsIgnoreCase(entry.topic)) {
                    found = true;
                    // Keep the one with highest confidence
                    if (entry.confidence > existing.confidence) {
                        list.set(i, entry);
                    }
                    break;
                }
            }
            if (!found) {
                list.add(entry);
            }
            // ------------------------------

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);

        } catch (java.io.IOException ex) {
            System.err.println("Error guardando conocimiento JSON: " + ex.getMessage());
        }
    }

    private void updateTopKnowledge() {
        try {
            java.io.File file = new java.io.File("memory/data/knowledge.json");
            if (file.exists()) {
                List<KnowledgeEntry> all = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<KnowledgeEntry>>() {});
                all.sort((a, b) -> Double.compare(b.confidence, a.confidence));
                topKnowledge = all.stream().limit(5).collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {}
    }

    private void drawTopKnowledgePanel() {
        int x = SCREEN_WIDTH/2 - 200;
        int y = 10;
        drawRectangleRounded(new Rectangle().x(x).y(y).width(400).height(110), 0.2f, 8, fade(DARKGRAY, 0.8f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(400).height(110), 0.2f, 8, fade(GOLD, 0.5f));
        drawText("TOP 5 CONOCIMIENTO CRÍTICO (MAX CONFIDENCE)", x + 10, y + 10, 10, GOLD);
        
        for (int i = 0; i < topKnowledge.size(); i++) {
            KnowledgeEntry k = topKnowledge.get(i);
            String txt = (i+1) + ". " + k.topic + " (" + String.format("%.2f", k.confidence) + ")";
            drawText(txt, x + 15, y + 30 + (i * 15), 11, RAYWHITE);
        }
    }

    private void drawChatWindow() {
        int w = 400; int h = 250;
        int x = 280; int y = SCREEN_HEIGHT - h - 10;
        drawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(h), 0.1f, 8, fade(BLACK, 0.8f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(w).height(h), 0.1f, 8, SKYBLUE);
        drawText("CHAT CON JETTRA WOLF (EL LÍDER)", x + 15, y + 15, 14, SKYBLUE);
        
        // History
        for (int i = 0; i < chatHistory.size(); i++) {
            if (i > 8) break;
            drawText("> " + chatHistory.get(chatHistory.size() - 1 - i), x + 15, y + h - 60 - (i * 18), 12, RAYWHITE);
        }

        // Input Box
        drawRectangle(x + 10, y + h - 35, w - 20, 25, DARKGRAY);
        drawRectangleLines(x + 10, y + h - 35, w - 20, 25, RAYWHITE);
        drawText(chatInput + "_", x + 20, y + h - 28, 12, LIME);
        drawText("[ENTER] para enviar", x + 150, y + 20, 10, GRAY);
    }

    private void sendChatMessage(String msg) {
        chatHistory.add("Tú: " + msg);
        // Find Jettra
        for (HumanEntity e : entities) {
            if (e.name.contains("Jettra")) {
                String res = "Soy el Lobo Jettra. Tu mensaje ha sido recibido.";
                if (msg.toLowerCase().contains("hola")) res = "Saludos, mortal. El conocimiento fluye.";
                if (msg.toLowerCase().contains("construye")) res = "¡Mis agentes ya están en ello!";
                if (msg.toLowerCase().contains("quien eres")) res = "Soy el guía de este mundo 3D.";

                // Physical Commands
                if (msg.toLowerCase().contains("teletransportar")) {
                    res = "Realizando teletransporte cuántico al centro del mapa.";
                    for (HumanEntity target : entities) {
                        if (!target.name.contains("Jettra")) {
                            target.x = 0; target.z = 0; target.targetX = 0; target.targetZ = 0;
                        }
                    }
                    worldEvents.add(new WorldEvent("Teletransporte masivo activado por Jettra", worldTime, 255, 255, 0));
                }
                if (msg.toLowerCase().contains("poblacion")) {
                    res = "Invocando nuevos agentes al mundo...";
                    for(int i=0; i<5; i++) {
                        generateEntity("Sentry-" + (int)(Math.random()*1000), (float)(Math.random()*20-10), 0, (float)(Math.random()*20-10), false, false, false);
                    }
                }
                if (msg.toLowerCase().contains("limpiar")) {
                    res = "Limpiando todos los proyectos y artefactos obsoletos.";
                    artifacts.clear();
                    megaProjects.clear();
                    worldEvents.add(new WorldEvent("Mundo purificado por el líder", worldTime, 100, 100, 255));
                }
                if (msg.toLowerCase().contains("acelerar")) {
                    timeScale = (timeScale == 1.0f) ? 5.0f : 1.0f;
                    res = "Escala de tiempo ajustada a " + timeScale + "x.";
                    worldEvents.add(new WorldEvent("Manipulación temporal: " + res, worldTime, 0, 255, 200));
                }
                if (msg.toLowerCase().contains("clima")) {
                    weatherMode = (weatherMode + 1) % 3;
                    String[] m = {"Soleado", "Nocturno", "Tormentoso"};
                    res = "Cambiando entorno a modo " + m[weatherMode] + ".";
                    worldEvents.add(new WorldEvent("Cambio climático: " + res, worldTime, 200, 0, 255));
                }
                
                final String finalRes = res;
                chatHistory.add("Jettra: " + finalRes);
                e.currentThought = finalRes;
                e.thoughtTimer = 5.0f;
                thoughts.add(new Thought() {{
                    content = finalRes; x = e.x; y = e.y + 3.5f; z = e.z;
                    timer = 5.0f; isThought = false;
                }});
                break;
            }
        }
        if (chatHistory.size() > 20) chatHistory.remove(0);
    }

    private void initSfx() {
        // Here we could loadSound("rain.wav") or generate procedural waves.
        // For simplicity and to avoid crashes with missing files or JNA-Wave complex buffers, 
        // we'll use a simulation with world events that emit a 'play' signal.
    }

    private void updateSfx() {
        if (!sfxEnabled) return;
        // In reality, this would play/stop a loop based on weatherMode.
    }

    public static void main(String[] args) {
        new Jettra3DApp().run();
    }
}
