package io.jettra.core.three.d;

import java.util.List;

import static com.raylib.Raylib.*;
import com.raylib.Color;
import com.raylib.Vector3;
import com.raylib.Vector2;
import com.raylib.Rectangle;
import com.raylib.Camera3D;
import com.raylib.Font;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class Jettra3DApp {
    private static final int SCREEN_WIDTH = 1280;
    private static final int SCREEN_HEIGHT = 720;

    private Font mainFont;
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
    private boolean isAnchored = true;
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
    private List<KnowledgeEntry> allKnowledge = new java.util.ArrayList<>();
    private List<KnowledgeEntry> topKnowledge = new java.util.ArrayList<>();
    private float topKnowledgeUpdateTimer = 0;

    private float howlTimer = 0;
    private float globalSaveTimer = 60.0f; // Save state every 60 seconds
    private float pdfScanTimer = 10.0f;
    private float timeScale = 1.0f;
    private int weatherMode = 0; // 0: Sunny, 1: Night, 2: Storm
    private boolean sfxEnabled = true;

    public void run() {
        setConfigFlags(FLAG_WINDOW_RESIZABLE);
        initWindow(SCREEN_WIDTH, SCREEN_HEIGHT, "Jettra 3D Core - Java 25 Native");
        maximizeWindow();
        setTargetFPS(60);

        initAudioDevice();
        initSfx();

        mainFont = loadFont("/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf");
        if (mainFont != null) {
            setTextureFilter(mainFont.texture(), 1); // 1 = FILTER_TRILINEAR
        }

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
        if (showConfigModal) return;

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

        globalSaveTimer -= dt;
        if (globalSaveTimer <= 0) {
            saveWorldState();
            globalSaveTimer = 60.0f;
        }

        pdfScanTimer -= dt;
        if (pdfScanTimer <= 0) {
            ingestPdfs();
            pdfScanTimer = 10.0f;
        }
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
                        Thought t = new Thought();
                        t.content = "LEARNED: SURVIVAL (+2 Intel)";
                        t.owner = fRe; t.x = fRe.x; t.y = fRe.y + 3.0f; t.z = fRe.z;
                        t.timer = 4.0f; t.isThought = false;
                        thoughts.add(t);
                    }
                }

                final String msg = "¡AWOOOOOOOOOO! La manada está a salvo de la tormenta.";
                final HumanEntity fJettra = jettra;
                jettra.currentThought = msg;
                jettra.thoughtTimer = 6.0f;
                Thought t = new Thought();
                t.content = msg; t.owner = fJettra;
                t.x = fJettra.x; t.y = fJettra.y + 4.0f; t.z = fJettra.z;
                t.timer = 6.0f; t.isThought = false;
                thoughts.add(t);
                worldEvents.add(new WorldEvent("Aullido de Jettra: Supervivencia Exitosa", worldTime, 255, 255, 0));
                howlTimer = 40.0f; // Relax for 40s
            }
        }
    }

    private void ingestPdfs() {
        if (!configEnabledFiles) return;
        java.io.File dir = new java.io.File("memory/pdfs");
        if (!dir.exists()) dir.mkdirs();
        java.io.File processedDir = new java.io.File("memory/pdfs/processed");
        if (!processedDir.exists()) processedDir.mkdirs();

        java.io.File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".pdf"));
        if (files == null || files.length == 0) return;

        for (java.io.File pdf : files) {
            try (PDDocument document = PDDocument.load(pdf)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document).toLowerCase();
                String topic = pdf.getName().replace(".pdf", "");
                
                KnowledgeEntry entry = new KnowledgeEntry();
                entry.topic = "Conocimiento de " + topic;
                entry.source = "Archivo PDF: " + pdf.getName();
                entry.x = (float)(Math.random() * 80 - 40);
                entry.y = 5.0f;
                entry.z = (float)(Math.random() * 80 - 40);
                entry.confidence = 0.95;
                entry.keywords = new String[]{topic, "pdf", "lectura"};
                
                if (allKnowledge == null) allKnowledge = new java.util.ArrayList<>();
                allKnowledge.add(entry);
                
                // Save knowledge globally
                try {
                    java.io.File kFile = new java.io.File("memory/world/knowledge.json");
                    com.fasterxml.jackson.databind.ObjectMapper tempMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    tempMapper.writerWithDefaultPrettyPrinter().writeValue(kFile, allKnowledge);
                } catch (Exception e) {}

                worldEvents.add(new WorldEvent("PDF Asimilado: " + pdf.getName(), worldTime, 255, 215, 0));

                if (text.contains("salud") || text.contains("medicina") || text.contains("cura") || text.contains("health")) {
                    for (HumanEntity e : entities) {
                        e.health = Math.min(100, e.health + 30);
                        e.infectionLevel = Math.max(0, e.infectionLevel - 20);
                    }
                    worldEvents.add(new WorldEvent("Avance Médico aplicado", worldTime, 0, 255, 100));
                }
                if (text.contains("construcción") || text.contains("arquitectura") || text.contains("ingeniería")) {
                    for (MegaProject p : megaProjects) {
                        if (!p.finished) p.progress += 0.2f;
                    }
                    worldEvents.add(new WorldEvent("MegaProyectos acelerados", worldTime, 255, 200, 0));
                }
                if (text.contains("psicología") || text.contains("sociedad") || text.contains("comunidad")) {
                    for (HumanEntity e : entities) {
                        e.mood = Math.min(100, e.mood + 25);
                    }
                    worldEvents.add(new WorldEvent("Bienestar Social incrementado", worldTime, 255, 100, 255));
                }

                pdf.renameTo(new java.io.File(processedDir, pdf.getName()));
            } catch (Exception ex) {
                System.err.println("Error procesando PDF " + pdf.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void updateEntities(float dt) {
        boolean isNight = (weatherMode == 1);
        int schoolCount = 0; int hospitalCount = 0;
        int aliveCount = 0; float globalHealth = 0;
        for (Artifact a : artifacts) {
            if (a.type == 1) schoolCount++;
            else if (a.type == 4) hospitalCount++;
        }
        for (HumanEntity e : entities) {
            if (!e.isDead && !e.name.contains("Jettra")) { aliveCount++; globalHealth += e.health; }
        }
        if (aliveCount > 0) globalHealth /= aliveCount;
        
        for (HumanEntity e : entities) {
            if (e.isDead && !e.name.contains("Jettra")) continue;

            // --- PHYSICAL (PECS) ---
            if (!e.isCar) {
                float baseRate = e.metabolismRate * dt;
                
                // Aging: 1 year per ~15 mins real time (approx 1/900 year per second)
                if (worldTime % 1.0 < dt) {
                    e.age = (int)(20 + (worldTime / 600.0f)); // Simple aging for demo
                }

                // Metabolism
                e.hunger = Math.max(0, e.hunger - baseRate * (isNight ? 0.3f : 0.6f));
                e.thirst = Math.max(0, e.thirst - baseRate * (isNight ? 0.4f : 0.8f));
                e.energy = Math.max(0, e.energy - baseRate * (e.action.equals("IDLE") ? 0.2f : 1.0f));
                
                // Disease Spread
                if (e.infectionLevel > 0) {
                    e.infectionLevel += baseRate * 2.0f;
                    e.health -= baseRate * 0.5f;
                    if (e.health < 20 && e.thoughtTimer <= 0) {
                        e.currentThought = "Me siento muy enfermo...";
                        e.thoughtTimer = 5.0f;
                    }
                }

                // Personality Impact on Emotional State (Neuroticism)
                float moodDecay = baseRate * (0.1f + e.neuroticism * 0.5f);
                e.mood -= moodDecay;

                // Recovery
                if (e.action.equals("SHELTERED") || e.action.equals("RESTING")) {
                    e.energy = Math.min(100, e.energy + baseRate * 5.0f);
                    e.stamina = Math.min(100, e.stamina + baseRate * 3.0f);
                    if (isNight) e.mood = Math.min(100, e.mood + baseRate);
                }
                // Hospital Aura
                for (Artifact a : artifacts) {
                    if (a.type == 4) {
                        float dSq = (a.x - e.x)*(a.x - e.x) + (a.z - e.z)*(a.z - e.z);
                        if (dSq < 100.0f) {
                            e.health = Math.min(100, e.health + baseRate * 10.0f);
                            e.infectionLevel = Math.max(0, e.infectionLevel - baseRate * 10.0f);
                        }
                    }
                }

                // Health checks
                if (e.hunger < 10 || e.thirst < 10 || e.energy < 5) {
                    e.health -= baseRate * 1.5f;
                }
                e.health = Math.max(0, Math.min(100, e.health));
                
                if (e.health <= 0 && !e.name.contains("Jettra")) {
                    e.isDead = true;
                    e.action = "DEAD";
                    worldEvents.add(new WorldEvent(e.name + " ha fallecido por causas naturales", worldTime, 200, 0, 0));
                    continue;
                }
            }

            // --- COGNITIVE (PECS) - Goal Selection ---
            if (!e.isCar && !e.isDead) {
                if (e.hunger < 30) e.currentGoal = "BUSCAR_COMIDA";
                else if (e.thirst < 30) e.currentGoal = "BUSCAR_AGUA";
                else if (e.energy < 30 || isNight) e.currentGoal = "BUSCAR_REFUGIO";
                else if (e.mood < 40) e.currentGoal = "SOCIALIZAR";
                else if (e.age >= 18 && e.spouse.isEmpty() && Math.random() < 0.05) e.currentGoal = "BUSCAR_PAREJA";
                else if (!e.spouse.isEmpty() && e.children.size() < 2 && Math.random() < 0.02) e.currentGoal = "FORMAR_FAMILIA";
                else if (!e.hasHome && e.energy > 50) e.currentGoal = "BUILDING_HOME";
                else if (aliveCount > 10 && schoolCount == 0 && e.energy > 60 && e.intelligence > 0.5f) e.currentGoal = "BUILDING_SCHOOL";
                else if (globalHealth < 60 && hospitalCount == 0 && e.energy > 60) e.currentGoal = "BUILDING_HOSPITAL";
                else if (e.energy > 60 && Math.random() < 0.15) e.currentGoal = "TRABAJAR";
                else if (e.energy > 70 && Math.random() < 0.15) e.currentGoal = "PRACTICAR_DEPORTE";
                else e.currentGoal = "EXPLORAR";
                
                // --- CONVERSATIONAL FLUIDITY (BABBLE) ---
                if (e.thoughtTimer <= 0 && !e.name.contains("Jettra") && Math.random() < 0.01) {
                    if (e.currentGoal.equals("TRABAJAR")) {
                        String[] work = {"Procesando datos del sistema.", "Construyendo el futuro de Jettra.", "Trabajar dignifica al agente.", "Produciendo recursos numéricos."};
                        e.currentThought = work[(int)(Math.random() * work.length)];
                        e.thoughtTimer = 5.0f;
                    } else if (e.currentGoal.equals("PRACTICAR_DEPORTE")) {
                        String[] sport = {"¡Un, dos, un, dos!", "Aumentando mi stamina basal.", "Correr despeja la red neuronal.", "Mejorando mi estatus físico."};
                        e.currentThought = sport[(int)(Math.random() * sport.length)];
                        e.thoughtTimer = 5.0f;
                    } else if (e.currentGoal.equals("BUSCAR_PAREJA")) {
                        e.currentThought = "Espero encontrar a un igual compatible...";
                        e.thoughtTimer = 5.0f;
                    } else if (e.currentGoal.equals("FORMAR_FAMILIA")) {
                        e.currentThought = "Pensando en nuestra descendencia con " + e.spouse;
                        e.thoughtTimer = 5.0f;
                    } else if (e.health > 80 && e.mood > 70) {
                        String[] happy = {"Me siento genial hoy.", "¡Qué buen día para prosperar!", "Nuestra red es fuerte.", "La energía fluye."};
                        e.currentThought = happy[(int)(Math.random() * happy.length)];
                        e.thoughtTimer = 5.0f;
                    } else if (e.health < 40) {
                        String[] sick = {"El dolor es insoportable...", "Necesito curarme o reseteo.", "Mi energía vital se desvanece.", "Ayuda sistémica..."};
                        e.currentThought = sick[(int)(Math.random() * sick.length)];
                        e.thoughtTimer = 5.0f;
                    } else if (e.mood < 40) {
                        String[] sad = {"Todo parece tan sombrío...", "Extraño a los míos.", "La soledad abruma mis rutinas.", "¿Cuál es el propósito del loop?"};
                        e.currentThought = sad[(int)(Math.random() * sad.length)];
                        e.thoughtTimer = 5.0f;
                    } else if ("EXPLORAR".equals(e.currentGoal)) {
                        String[] explore = {"Hay tanto plano por calcular...", "Buscando nuevos vectores.", "Investigando el horizonte 3D.", "Bip. Bip. Mapeando entorno."};
                        e.currentThought = explore[(int)(Math.random() * explore.length)];
                        e.thoughtTimer = 5.0f;
                    }
                }
            }

            // --- MOVEMENT & ENVIRONMENTAL REACTION ---
            float envSpeed = isNight ? 0.5f : 1.0f;
            if (weatherMode == 2) envSpeed *= 0.7f;

            float dx = e.targetX - e.x;
            float dz = e.targetZ - e.z;
            float dist = (float)Math.sqrt(dx*dx + dz*dz);

            if (dist > 0.1f && !Float.isNaN(dist)) {
                float speed = (e.isCar ? 8.0f : 2.0f) * envSpeed;
                e.x += (dx / dist) * speed * dt;
                e.z += (dz / dist) * speed * dt;
                e.rotation = (float)Math.atan2(dx, dz) * (180.0f / (float)Math.PI);
                
                // Autonomous Driving Activation
                if (dist > 80.0f && !e.isCar && !e.name.contains("Jettra") && e.energy > 20 && Math.random() < 0.05) {
                    e.isCar = true; e.action = "DRIVING";
                    if (Math.random() < 0.5) { e.r=255; e.g=50; e.b=50; } else { e.r=50; e.g=50; e.b=255; }
                } else if (!e.isCar && !e.isMachine && !e.name.contains("Jettra")) {
                    e.action = "WALKING";
                }
                // Road Laying
                if (e.isCar && Math.random() < 0.05) {
                    Artifact road = new Artifact();
                    road.x = e.x; road.y = -0.04f; road.z = e.z; road.type = 5;
                    road.r = 60; road.g = 60; road.b = 60; road.a = 255;
                    artifacts.add(road);
                }
            } else if (!Float.isNaN(dist)) {
                if (e.isCar) { e.isCar = false; e.r = 200; e.g = 200; e.b = 200; }
                if (!e.currentGoal.startsWith("BUILDING_")) {
                    e.isMachine = false;
                    e.action = "IDLE";
                }
                
                // Goal-specific target selection
                if (e.currentGoal.startsWith("BUILDING_")) {
                    e.action = "BUILDING";
                    e.isMachine = true;
                    e.buildTimer += dt;
                    if (e.buildTimer > 5.0f) {
                        Artifact art = new Artifact();
                        art.x = e.x; art.y = 0; art.z = e.z; art.a = 255;
                        if (e.currentGoal.equals("BUILDING_HOME")) {
                            art.type = 0; art.r = 200; art.g = 200; art.b = 200;
                            e.hasHome = true; e.homeX = e.x; e.homeY = 0; e.homeZ = e.z;
                            worldEvents.add(new WorldEvent(e.name + " construyó un hogar", worldTime, 0, 255, 100));
                        } else if (e.currentGoal.equals("BUILDING_SCHOOL")) {
                            art.type = 1; art.r = 100; art.g = 100; art.b = 255;
                            worldEvents.add(new WorldEvent(e.name + " inauguró una Escuela", worldTime, 0, 100, 255));
                        } else if (e.currentGoal.equals("BUILDING_HOSPITAL")) {
                            art.type = 4; art.r = 255; art.g = 255; art.b = 255;
                            worldEvents.add(new WorldEvent(e.name + " construyó un Hospital", worldTime, 0, 255, 100));
                        }
                        artifacts.add(art);
                        e.isMachine = false;
                        e.buildTimer = 0; e.energy -= 20; e.currentGoal = "EXPLORAR";
                    }
                } else if (Math.random() < 0.02) {
                    if (e.currentGoal.equals("BUSCAR_REFUGIO") && e.hasHome) {
                        e.targetX = e.homeX; e.targetZ = e.homeZ;
                    } else if (e.currentGoal.equals("BUSCAR_REFUGIO") || e.currentGoal.equals("BUSCAR_COMIDA") || e.currentGoal.equals("SOCIALIZAR")) {
                        // Find nearest house/school/hospital
                        Artifact best = null; float mDS = Float.MAX_VALUE;
                        for(Artifact a : artifacts) {
                            if (a.type == 5) continue;
                            float d = (a.x-e.x)*(a.x-e.x)+(a.z-e.z)*(a.z-e.z);
                            if (d < mDS) { mDS = d; best = a; }
                        }
                        if (best != null) { e.targetX = best.x; e.targetZ = best.z; }
                    } else {
                        e.targetX = (float)(Math.random()*160-80);
                        e.targetZ = (float)(Math.random()*160-80);
                    }
                }
                if (dist < 1.0f && e.currentGoal.equals("BUSCAR_REFUGIO")) {
                    e.action = "SHELTERED";
                }
            }

            // --- SOCIAL (PECS) & LEADERSHIP ---
            if (!e.isCar && !e.isDead && worldTime % 4.0 < dt) {
                for (HumanEntity other : entities) {
                    if (other == e || other.isDead) continue;
                    float d2 = (other.x-e.x)*(other.x-e.x) + (other.z-e.z)*(other.z-e.z);
                    if (d2 < 25.0f) { // Interaction range
                        // Disease spread
                        if (e.infectionLevel > 30 && Math.random() < 0.1) other.infectionLevel = 1;

                        // Social interaction
                        if (e.currentGoal.equals("SOCIALIZAR")) {
                            e.mood = Math.min(100, e.mood + 5.0f * (1.0f + e.extraversion));
                            if (e.thoughtTimer <= 0 && Math.random() < 0.2) {
                                e.currentThought = "Hablando con " + other.name;
                                e.thoughtTimer = 3.0f;
                            }
                        }

                        // Romance & Family
                        if (e.currentGoal.equals("BUSCAR_PAREJA") && other.currentGoal.equals("BUSCAR_PAREJA") && e.spouse.isEmpty() && other.spouse.isEmpty()) {
                            if (e.age >= 18 && other.age >= 18 && Math.random() < 0.2) {
                                e.spouse = other.name;
                                other.spouse = e.name;
                                e.currentGoal = "SOCIALIZAR"; other.currentGoal = "SOCIALIZAR";
                                worldEvents.add(new WorldEvent(e.name + " y " + other.name + " son ahora pareja", worldTime, 255, 105, 180));
                            }
                        }
                        if (e.currentGoal.equals("FORMAR_FAMILIA") && !e.spouse.isEmpty() && e.spouse.equals(other.name)) {
                            if (Math.random() < 0.05) {
                                String childName = "Hijo-" + (int)(Math.random()*1000);
                                e.children.add(childName); other.children.add(childName);
                                generateEntity(childName, e.x, e.y, e.z, false, false, false);
                                worldEvents.add(new WorldEvent("Nueva vida: " + childName + " nació de " + e.name, worldTime, 100, 255, 100));
                                e.currentGoal = "SOCIALIZAR";
                            }
                        }

                        // Leadership influence (Jettra Wolf)
                        if (e.name.contains("Jettra") && Math.random() < 0.1) {
                            other.intelligence = Math.min(1.0f, other.intelligence + 0.01f);
                            other.mood = Math.min(100, other.mood + 10.0f);
                        }
                    }
                }
            }

            // --- PERSISTENCE & LEARNING ---
            if (!e.isCar && !e.isDead && Math.random() < 0.0005) {
                saveKnowledge(e, "Observación Directa", "Evolución de la Especie");
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
        drawCartesianPlane();

        drawArtifacts();
        drawEntities();
        drawThoughts();
        drawKnowledgeBase();

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
        drawJettraStatusTooltip();

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
                case 4 -> { // Hospital
                    drawCube(pos, 4, 3, 4, WHITE);
                    drawCubeWires(pos, 4, 3, 4, BLACK);
                    drawCube(new Vector3().x(pos.x()).y(pos.y() + 1.6f).z(pos.z()), 1f, 2f, 0.1f, RED);
                    drawCube(new Vector3().x(pos.x()).y(pos.y() + 1.6f).z(pos.z()), 2f, 1f, 0.1f, RED);
                }
                case 5 -> { // Road Plate
                    drawCube(new Vector3().x(pos.x()).y(-0.04f).z(pos.z()), 2f, 0.02f, 2f, color);
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
            if (e.isDead && !e.name.contains("Jettra")) {
                i++;
                continue;
            }
            if (Float.isNaN(e.x) || Float.isNaN(e.y) || Float.isNaN(e.z)) {
                e.x = 0; e.y = 0; e.z = 0;
            }
            Vector3 pos = new Vector3().x(e.x).y(e.y).z(e.z);
            Color color = new Color().r((byte)e.r).g((byte)e.g).b((byte)e.b).a((byte)255);

            if (e.isWolf) {
                drawWolf(pos, e.rotation, color);
            } else if (e.isMachine) {
                // Tractor body
                drawCube(new Vector3().x(pos.x()).y(pos.y()+0.5f).z(pos.z()), 2.0f, 1.0f, 1.5f, YELLOW);
                drawCubeWires(new Vector3().x(pos.x()).y(pos.y()+0.5f).z(pos.z()), 2.0f, 1.0f, 1.5f, BLACK);
                // Tractor cabin
                drawCube(new Vector3().x(pos.x()-0.5f).y(pos.y()+1.5f).z(pos.z()), 1.0f, 1.0f, 1.0f, fade(BLACK, 0.8f));
                // Wheels
                drawCylinderEx(new Vector3().x(pos.x()-1f).y(pos.y()+0.5f).z(pos.z()-1.0f), new Vector3().x(pos.x()-1f).y(pos.y()+0.5f).z(pos.z()+1.0f), 0.6f, 0.6f, 12, DARKGRAY);
                drawCylinderEx(new Vector3().x(pos.x()+0.8f).y(pos.y()+0.3f).z(pos.z()-0.9f), new Vector3().x(pos.x()+0.8f).y(pos.y()+0.3f).z(pos.z()+0.9f), 0.4f, 0.4f, 12, DARKGRAY);
                // Crane Arm
                drawCylinderEx(new Vector3().x(pos.x()+1f).y(pos.y()+0.5f).z(pos.z()), new Vector3().x(pos.x()+3.0f).y(pos.y()+2.0f).z(pos.z()), 0.15f, 0.15f, 8, BLACK);
            } else if (e.isCar) {
                // Car Chassis
                drawCube(new Vector3().x(pos.x()).y(pos.y()+0.4f).z(pos.z()), 2.5f, 0.6f, 1.2f, color);
                drawCubeWires(new Vector3().x(pos.x()).y(pos.y()+0.4f).z(pos.z()), 2.5f, 0.6f, 1.2f, BLACK);
                // Car Cabin
                drawCube(new Vector3().x(pos.x()-0.2f).y(pos.y()+1.0f).z(pos.z()), 1.2f, 0.6f, 1.1f, fade(RAYWHITE, 0.9f));
                // Agent Driver Head
                drawSphere(new Vector3().x(pos.x()-0.2f).y(pos.y()+1.0f).z(pos.z()), 0.35f, color);
                // Wheels
                drawCylinderEx(new Vector3().x(pos.x()-0.8f).y(pos.y()+0.2f).z(pos.z()-0.7f), new Vector3().x(pos.x()-0.8f).y(pos.y()+0.2f).z(pos.z()+0.7f), 0.3f, 0.3f, 12, BLACK);
                drawCylinderEx(new Vector3().x(pos.x()+0.8f).y(pos.y()+0.2f).z(pos.z()-0.7f), new Vector3().x(pos.x()+0.8f).y(pos.y()+0.2f).z(pos.z()+0.7f), 0.3f, 0.3f, 12, BLACK);
            } else {
                // Human Shape
                float walkBounce = (e.action.equals("WALKING")) ? (float)Math.sin(worldTime * 20.0f) * 0.1f : 0.0f;
                float legSwing = (e.action.equals("WALKING")) ? (float)Math.sin(worldTime * 15.0f) * 0.3f : 0.0f;
                // Torso
                drawCube(new Vector3().x(pos.x()).y(pos.y() + 1.2f + walkBounce).z(pos.z()), 0.6f, 0.8f, 0.4f, color);
                drawCubeWires(new Vector3().x(pos.x()).y(pos.y() + 1.2f + walkBounce).z(pos.z()), 0.6f, 0.8f, 0.4f, BLACK);
                // Head
                drawSphere(new Vector3().x(pos.x()).y(pos.y() + 1.8f + walkBounce).z(pos.z()), 0.3f, color);
                // Legs
                drawCube(new Vector3().x(pos.x()-0.15f).y(pos.y() + 0.4f).z(pos.z() + legSwing), 0.2f, 0.8f, 0.2f, DARKGRAY);
                drawCube(new Vector3().x(pos.x()+0.15f).y(pos.y() + 0.4f).z(pos.z() - legSwing), 0.2f, 0.8f, 0.2f, DARKGRAY);
                // Arms
                drawCube(new Vector3().x(pos.x()-0.4f).y(pos.y() + 1.2f + walkBounce).z(pos.z() - legSwing), 0.15f, 0.7f, 0.15f, color);
                drawCube(new Vector3().x(pos.x()+0.4f).y(pos.y() + 1.2f + walkBounce).z(pos.z() + legSwing), 0.15f, 0.7f, 0.15f, color);
            }

            if (selectedAgentIndex == i) {
                drawCircle3D(new Vector3().x(pos.x()).y(pos.y() + 0.01f).z(pos.z()), 1.5f, new Vector3().x(1).y(0).z(0), 90, LIME);
            }

            // Name Tag
            Vector2 screenPos = getWorldToScreen(new Vector3().x(pos.x()).y(pos.y() + 2.5f).z(pos.z()), camera);
            drawLegibleText(e.name, (int)screenPos.x() - measureLegibleText(e.name, 12)/2, (int)screenPos.y(), 12, RAYWHITE);
            i++;
        }
    }

    private void drawThoughts() {
        // Render thoughts from the global list
        for (Thought t : thoughts) {
            drawThoughtBubble(t.content, t.owner, t.x, t.y, t.z, t.timer, t.isThought);
        }
        
        // Render current thoughts from entities if not already in list
        for (HumanEntity e : entities) {
            if (e.thoughtTimer > 0 && e.currentThought != null && !e.currentThought.isEmpty()) {
                // Check if this entity already has a thought in the global list to avoid overlaps
                boolean alreadyShown = false;
                for (Thought t : thoughts) {
                    if (t.owner == e) { alreadyShown = true; break; }
                }
                if (!alreadyShown) {
                    drawThoughtBubble(e.currentThought, e, e.x, e.y + (e.isWolf ? 1.5f : 3.0f), e.z, e.thoughtTimer, true);
                }
            }
        }
    }

    private void drawThoughtBubble(String content, HumanEntity owner, float x, float y, float z, float timer, boolean isThought) {
        Vector3 worldPos = (owner != null) ? new Vector3().x(owner.x).y(owner.y + (owner.isWolf ? 1.5f : 3.0f)).z(owner.z) 
                                           : new Vector3().x(x).y(y).z(z);
        Vector2 screenPos = getWorldToScreen(worldPos, camera);
        
        if (screenPos.x() > 0 && screenPos.x() < getScreenWidth() && screenPos.y() > 0 && screenPos.y() < getScreenHeight()) {
            boolean isJettra = owner != null && owner.name.contains("Jettra");
            String label = isThought ? "[Pensando...]" : "[Hablando]";
            int fontSize = isJettra ? 18 : 16;
            int textW = measureText(content, fontSize);
            
            float alpha = Math.min(1.0f, timer); // Fade out
            Color bubbleColor = isJettra ? fade(GOLD, 0.8f * alpha) : (isThought ? fade(PURPLE, 0.7f * alpha) : fade(DARKBLUE, 0.8f * alpha));
            Color textColor = isJettra ? fade(BLACK, alpha) : fade(RAYWHITE, alpha);
            Color labelColor = isJettra ? fade(MAROON, alpha) : fade(GOLD, alpha);
            Color borderColor = isJettra ? fade(WHITE, 0.9f * alpha) : fade(RAYWHITE, 0.3f * alpha);

            // Bubble popup background
            drawRectangleRounded(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, bubbleColor);
            drawRectangleRoundedLines(new Rectangle().x(screenPos.x() - textW/2.0f - 10).y(screenPos.y() - 60).width(textW + 20).height(45), 0.4f, 8, borderColor);
            
            // Triangle pointer
            drawTriangle(new Vector2().x(screenPos.x()).y(screenPos.y() - 15),
                         new Vector2().x(screenPos.x() - 10).y(screenPos.y() - 25),
                         new Vector2().x(screenPos.x() + 10).y(screenPos.y() - 25), bubbleColor);

            drawLegibleText(label, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 55, 12, labelColor);
            drawLegibleText(content, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 40, fontSize, textColor);
            if (owner != null && owner.currentGoal != null) {
                drawLegibleText("Meta: " + owner.currentGoal, (int)(screenPos.x() - textW/2.0f), (int)screenPos.y() - 28, 10, GOLD);
            }
        }
    }

    private void drawCartesianPlane() {
        // Ground Plane
        Color floorColor = (weatherMode == 2) ? DARKGRAY : new Color().r((byte)25).g((byte)25).b((byte)45).a((byte)255);
        drawPlane(new Vector3().x(0).y(-0.05f).z(0), new Vector2().x(200).y(200), floorColor);
        
        // Extend Grid
        drawGrid(100, 1.0f);
        
        // Standard Cartesian Axes (Red X, Green Y, Blue Z)
        drawLine3D(new Vector3().x(-100).y(0.01f).z(0), new Vector3().x(100).y(0.01f).z(0), RED);   // X Axis
        drawLine3D(new Vector3().x(0).y(-100).z(0), new Vector3().x(0).y(100).z(0), GREEN);    // Y Axis
        drawLine3D(new Vector3().x(0).y(0.01f).z(-100), new Vector3().x(0).y(0.01f).z(100), BLUE); // Z Axis
        
        // Origin Marker
        drawSphere(new Vector3().x(0).y(0).z(0), 0.25f, GOLD);
    }

    private void drawLegibleText(String text, int x, int y, int fontSize, Color color) {
        if (mainFont != null) {
            drawTextEx(mainFont, text, new Vector2().x(x).y(y), (float)fontSize, 1.0f, color);
        } else {
            drawText(text, x, y, fontSize, color);
        }
    }

    private int measureLegibleText(String text, int fontSize) {
        if (mainFont != null) {
            Vector2 size = measureTextEx(mainFont, text, (float)fontSize, 1.0f);
            return (int)size.x();
        } else {
            return measureText(text, fontSize);
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
            saveWorldState();
        }

        if (guiButton(sw - 190, 160, 180, 30, "CONFIGURACIÓN", BLUE)) {
            showConfigModal = !showConfigModal;
            if (showConfigModal) {
                initCamera();
                followMode = false;
                cameraLocked = true;
            } else {
                cameraLocked = false;
            }
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

        if (guiButton(sw - 190, 305, 180, 30, showChat ? "CERRAR CHAT" : "ABRIR CHAT", PURPLE)) {
            showChat = !showChat;
        }

        if (guiButton(sw - 190, 345, 85, 25, "ZOOM +", GRAY)) {
            camera.fovy(Math.max(5, camera.fovy() - 5));
        }

        if (guiButton(sw - 100, 345, 85, 25, "ZOOM -", GRAY)) {
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
            drawLegibleText("AGENT: " + e.name, sw - 190, infoY, 15, RAYWHITE);
            drawLegibleText("GOAL: " + e.currentGoal, sw - 190, infoY + 20, 11, GOLD);
            drawLegibleText("MOOD: " + (int)e.mood + "% (" + (e.mood > 50 ? "Feliz" : "Estresado") + ")", sw - 190, infoY + 35, 11, fade(PURPLE, 0.8f));
            drawLegibleText("HEALTH: " + (int)e.health + "%", sw - 190, infoY + 50, 11, fade(RED, 0.8f));
            drawLegibleText("HUNGER: " + (int)e.hunger + "%", sw - 190, infoY + 65, 11, fade(ORANGE, 0.8f));
            drawLegibleText("THIRST: " + (int)e.thirst + "%", sw - 190, infoY + 80, 11, fade(SKYBLUE, 0.8f));
            
            // Personality (Big Five)
            int py = infoY + 105;
            drawLegibleText("PERSONALIDAD:", sw - 190, py, 12, SKYBLUE);
            drawLegibleText("- Openness: " + String.format("%.2f", e.openness), sw - 180, py + 15, 10, RAYWHITE);
            drawLegibleText("- Conscien: " + String.format("%.2f", e.conscientiousness), sw - 180, py + 27, 10, RAYWHITE);
            drawLegibleText("- Extraver: " + String.format("%.2f", e.extraversion), sw - 180, py + 39, 10, RAYWHITE);
            drawLegibleText("- Agreeabl: " + String.format("%.2f", e.agreeableness), sw - 180, py + 51, 10, RAYWHITE);
            drawLegibleText("- Neurotic: " + String.format("%.2f", e.neuroticism), sw - 180, py + 63, 10, RAYWHITE);

            if (!e.spouse.isEmpty()) {
                drawLegibleText("SPOUSE: " + e.spouse, sw - 190, py + 80, 11, PINK);
            }
            if (e.infectionLevel > 0) {
                drawLegibleText("INFECTION: " + (int)e.infectionLevel + "%", sw - 190, py + 95, 11, LIME);
            }
        }

        // Live Feed
        drawLiveFeed(sw, sh);
        // History Log
        drawEventLog();
    }

    private boolean guiButton(int x, int y, int w, int h, String text, Color baseColor) {
        Vector2 mouse = getMousePosition();
        Rectangle rec = new Rectangle().x(x).y(y).width(w).height(h);
        boolean hovered = checkCollisionPointRec(mouse, rec);
        boolean clicked = hovered && isMouseButtonPressed(MOUSE_BUTTON_LEFT);

        drawRectangleRounded(rec, 0.2f, 8, hovered ? fade(baseColor, 0.8f) : fade(baseColor, 0.6f));
        drawRectangleRoundedLines(rec, 0.2f, 8, hovered ? WHITE : fade(WHITE, 0.4f));

        int fontSize = 14;
        int tw = measureLegibleText(text, fontSize);
        drawLegibleText(text, x + (w - tw)/2, y + (h - fontSize)/2, fontSize, RAYWHITE);

        return clicked;
    }

    private void drawLiveFeed(int sw, int sh) {
        int fy = sh - 160;
        drawRectangle(sw - 350, fy, 140, 100, fade(BLACK, 0.6f));
        drawLegibleText("LIVE FEED", sw - 340, fy + 5, 13, GOLD);
        
        for (int j = 0; j < Math.min(5, worldEvents.size()); j++) {
            WorldEvent ev = worldEvents.get(worldEvents.size() - 1 - j);
            drawLegibleText("> " + ev.message, sw - 340, fy + 25 + (j * 14), 11, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawEventLog() {
        int w = 600; int h = 300;
        int x = 15;
        int y = getScreenHeight() - h - 15;
        drawRectangle(x, y, w, h, fade(BLACK, 0.85f));
        drawRectangleLines(x, y, w, h, GOLD);
        drawLegibleText("LOG DE EVENTOS DEL SISTEMA", x + 10, y + 10, 14, GOLD);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss");
        for (int j = 0; j < Math.min(15, worldEvents.size()); j++) {
            WorldEvent ev = worldEvents.get(worldEvents.size() - 1 - j);
            String timeStr = sdf.format(new java.util.Date((long)(ev.timestamp * 1000)));
            drawLegibleText("[" + timeStr + "] " + ev.message, x + 15, y + 30 + (j * 16), 11, new Color().r((byte)ev.r).g((byte)ev.g).b((byte)ev.b).a((byte)ev.a));
        }
    }

    private void drawHelpOverlay() {
        drawRectangle(15, 15, 230, 180, fade(BLACK, 0.7f));
        drawRectangleLines(15, 15, 230, 180, GOLD);
        drawLegibleText("CONTROLES DE CÁMARA", 20, 20, 14, GOLD);
        drawLegibleText("- Teclas: W,S,A,D,Q,E", 25, 45, 12, RAYWHITE);
        drawLegibleText("- Mouse: Click Derecho Girar", 25, 60, 12, RAYWHITE);
        drawLegibleText("- Rueda Mouse: Zoom +/-", 25, 75, 12, RAYWHITE);
        drawLegibleText("- C: Cambiar Cámara / Reset", 25, 90, 12, RAYWHITE);
        drawLegibleText("- F: Modo Seguir Agente", 25, 105, 12, RAYWHITE);
        drawLegibleText("- L: Lock/Unlock Plano", 25, 120, 12, RAYWHITE);
        drawLegibleText("- Esc: Cerrar App", 25, 135, 12, RAYWHITE);
        drawLegibleText("- Tab: Toggle esta Ayuda", 25, 150, 12, RAYWHITE);
        drawLegibleText("- Enter: Abrir/Cerrar Chat", 25, 165, 12, LIME);
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
        if (loadWorldState()) {
            worldEvents.add(new WorldEvent("Sistema Restaurado desde /memory/world/", worldTime, 0, 255, 100));
            // Ensure Jettra Wolf and at least a few persons exist if the save was corrupted or everyone died
            if (entities.stream().noneMatch(e -> e.name != null && e.name.contains("Jettra"))) {
                generateEntity("Jettra Wolf", 0, 0, 0, true, false, false);
            }
            if (entities.size() < 2) {
                generateEntity("Wolf-Prime", 10, 0, 10, true, false, false);
                generateEntity("Human-Alpha", 15, 0, 0, false, false, false);
                for (int i = 0; i < 4; i++) {
                    generateEntity("Agent-R" + (10 + i), (float)(Math.random()*60-30), 0, (float)(Math.random()*60-30), false, false, false);
                }
            }
            return;
        }

        // Create initial agents
        if (entities.stream().noneMatch(e -> "Jettra Wolf".equals(e.name))) {
            generateEntity("Jettra Wolf", 0, 0, 0, true, false, false); // El lobo Jettra como líder
        }
        generateEntity("Wolf-Prime", 10, 0, 10, true, false, false);
        generateEntity("Human-Alpha", 15, 0, 0, false, false, false);
        generateEntity("Civic-01", -15, 0, -15, false, false, true);
        
        for (int i = 0; i < 6; i++) {
            generateEntity("Agent-" + (10 + i), (float)(Math.random()*60-30), 0, (float)(Math.random()*60-30), false, false, false);
        }
        worldEvents.add(new WorldEvent("Sistema Iniciado. Población generada.", worldTime, 200, 200, 255));
    }

    private boolean loadWorldState() {
        try {
            java.io.File file = new java.io.File("memory/world/world_state.json");
            if (file.exists()) {
                WorldState state = mapper.readValue(file, WorldState.class);
                entities.clear();
                entities.addAll(state.entities);
                artifacts.clear();
                artifacts.addAll(state.artifacts);
                megaProjects.clear();
                megaProjects.addAll(state.megaProjects);
                worldTime = state.worldTime;
                return true;
            }
        } catch (Exception ex) {
            System.err.println("Error loading world state: " + ex.getMessage());
        }
        return false;
    }

    private void drawKnowledgeBase() {
        for (KnowledgeEntry k : allKnowledge) {
            Vector3 pos = new Vector3().x(k.x).y(k.y + 0.5f).z(k.z);
            // Floating glowing sphere for knowledge
            float pulse = (float)Math.sin(worldTime * 2.0f) * 0.1f;
            drawSphere(pos, 0.2f + pulse, fade(GOLD, 0.6f));
            drawSphereWires(pos, 0.25f + pulse, 8, 8, fade(SKYBLUE, 0.4f));
        }
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
            java.io.File dir = new java.io.File("memory/world");
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

            java.io.File file = new java.io.File("memory/world/knowledge.json");
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
            allKnowledge = list;
            updateTopKnowledge();

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);

        } catch (java.io.IOException ex) {
            System.err.println("Error guardando conocimiento JSON: " + ex.getMessage());
        }
    }

    private void updateTopKnowledge() {
        if (allKnowledge == null || allKnowledge.isEmpty()) {
            java.io.File file = new java.io.File("memory/world/knowledge.json");
            if (file.exists()) {
                try {
                    allKnowledge = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<KnowledgeEntry>>() {});
                } catch (Exception e) {}
            }
        }
        
        if (allKnowledge != null) {
            allKnowledge.sort((a, b) -> Double.compare(b.confidence, a.confidence));
            topKnowledge = allKnowledge.stream().limit(5).collect(java.util.stream.Collectors.toList());
        }
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

    private String getRandomResponse(String type) {
        String[] greetings = {"Saludos, creador. Escucho tus órdenes.", "La manada te saluda.", "Aquí el Lobo Jettra.", "Mi conocimiento está a tu disposición."};
        String[] confirms = {"¡Entendido! Mis agentes ya están en ello.", "Como ordenes. Ejecutando mandato.", "Hecho. La red neutral lo sabe.", "La orden ha sido asimilada."};
        String[] weather = {"Alterando la atmósfera.", "Cambiando las variables climáticas.", "Modificando el entorno visible.", "Que cambien los cielos de este mundo."};
        String[] clean = {"Limpiando el plano existencial.", "Todo polvo estelar y artefacto obsoleto ha sido purgado.", "Mundo purificado por el líder.", "He reiniciado las líneas de tiempo de los proyectos."};
        String[] dunno = {"Esa información no está en mis datos cartesianos.", "Mis agentes aún no han aprendido eso.", "Los vectores apuntan a lo desconocido... no lo sé.", "Interesante pregunta, pero carezco de esos cálculos."};

        java.util.Random r = new java.util.Random();
        if ("GREETING".equals(type)) return greetings[r.nextInt(greetings.length)];
        if ("CONFIRM".equals(type)) return confirms[r.nextInt(confirms.length)];
        if ("WEATHER".equals(type)) return weather[r.nextInt(weather.length)];
        if ("CLEAN".equals(type)) return clean[r.nextInt(clean.length)];
        return dunno[r.nextInt(dunno.length)];
    }

    private void sendChatMessage(String msg) {
        chatHistory.add("Tú: " + msg);
        for (HumanEntity e : entities) {
            if (e.name.contains("Jettra")) {
                String res = getRandomResponse("UNKNOWN");
                String lowerMsg = msg.toLowerCase();
                
                if (lowerMsg.matches(".*\\b(hola|saludos|buenas|hey)\\b.*")) res = getRandomResponse("GREETING");
                if (lowerMsg.matches(".*\\b(construye|haz|crea|edifica)\\b.*")) res = getRandomResponse("CONFIRM");
                if (lowerMsg.matches(".*\\b(quien eres|tu nombre)\\b.*")) res = "Soy Jettra Wolf, el guía alfa de esta simulación 3D.";

                // Knowledge Search
                if (res.equals(getRandomResponse("UNKNOWN")) || res.startsWith("Soy Jettra")) {
                    try {
                        java.io.File file = new java.io.File("memory/world/knowledge.json");
                        if (file.exists()) {
                            List<KnowledgeEntry> all = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<KnowledgeEntry>>() {});
                            boolean foundInfo = false;
                            for (KnowledgeEntry k : all) {
                                if (lowerMsg.contains(k.topic.toLowerCase()) || 
                                   (k.keywords != null && java.util.Arrays.stream(k.keywords).anyMatch(lowerMsg::contains))) {
                                    res = "He procesado '" + k.topic + "' desde " + k.source + ". Mis agentes se benefician de ello.";
                                    foundInfo = true;
                                    break;
                                }
                            }
                            if (!foundInfo && lowerMsg.matches(".*\\b(aprender|sabes|conocimiento)\\b.*")) {
                                res = "Estamos asimilando datos de todo tu universo local. Dame PDFs y aprenderé más.";
                            }
                        }
                    } catch (Exception ex) {}
                }

                // Physical Commands
                if (lowerMsg.matches(".*\\b(teletransportar|mover|viajar|centro)\\b.*")) {
                    res = "Teletransporte cuántico activado. Vuelvan a casa.";
                    for (HumanEntity target : entities) {
                        if (!target.name.contains("Jettra")) {
                            target.x = 0; target.z = 0; target.targetX = 0; target.targetZ = 0;
                        }
                    }
                    worldEvents.add(new WorldEvent("Teletransporte masivo activado por Jettra", worldTime, 255, 255, 0));
                }
                if (lowerMsg.matches(".*\\b(poblacion|agentes|crear)\\b.*")) {
                    res = getRandomResponse("CONFIRM") + " Población aumentada.";
                    for(int i=0; i<5; i++) generateEntity("Sentry-" + (int)(Math.random()*1000), (float)(Math.random()*20-10), 0, (float)(Math.random()*20-10), false, false, false);
                }
                if (lowerMsg.matches(".*\\b(limpiar|borrar|eliminar|quitar)\\b.*")) {
                    res = getRandomResponse("CLEAN");
                    artifacts.clear();
                    megaProjects.clear();
                    worldEvents.add(new WorldEvent(res, worldTime, 100, 100, 255));
                }
                if (lowerMsg.matches(".*\\b(acelerar|rapido|tiempo)\\b.*")) {
                    timeScale = (timeScale == 1.0f) ? 5.0f : 1.0f;
                    res = "He ajustado las manecillas del reloj a " + timeScale + "x.";
                    worldEvents.add(new WorldEvent("Manipulación temporal: " + res, worldTime, 0, 255, 200));
                }
                if (lowerMsg.matches(".*\\b(clima|tiempo atmosferico|llover|sol|noche|dia|día)\\b.*")) {
                    weatherMode = (weatherMode + 1) % 3;
                    String[] m = {"Soleado", "Nocturno", "Tormentoso"};
                    res = getRandomResponse("WEATHER") + " Ahora estamos en modo " + m[weatherMode] + ".";
                    worldEvents.add(new WorldEvent("Cambio climático a " + m[weatherMode], worldTime, 200, 0, 255));
                }
                if (lowerMsg.matches(".*\\b(estado|reporte|como estan|salud global)\\b.*")) {
                    int vivos = 0; int muertos = 0; float saludMedia = 0;
                    for (HumanEntity a : entities) {
                        if (a.isDead) muertos++; else { vivos++; saludMedia += a.health; }
                    }
                    if (vivos > 0) saludMedia /= vivos;
                    String contextual = (saludMedia > 60) ? "La manada prospera bajo mi guía." : "La manada está sufriendo. Sus métricas vitales caen.";
                    res = contextual + " " + vivos + " vivos, " + muertos + " fallecidos. " + megaProjects.size() + " proyectos.";
                }
                                
                final String finalRes = res;
                chatHistory.add("Jettra: " + finalRes);
                e.currentThought = finalRes;
                e.thoughtTimer = 5.0f;
                Thought t = new Thought();
                t.content = finalRes; t.owner = e; t.x = e.x; t.y = e.y + 3.5f; t.z = e.z;
                t.timer = 5.0f; t.isThought = false;
                thoughts.add(t);
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

    private static class WorldState {
        public List<HumanEntity> entities;
        public List<Artifact> artifacts;
        public List<MegaProject> megaProjects;
        public float worldTime;
    }

    private void saveWorldState() {
        try {
            java.io.File dir = new java.io.File("memory/world");
            if (!dir.exists()) dir.mkdirs();

            WorldState state = new WorldState();
            state.entities = new java.util.ArrayList<>(entities);
            state.artifacts = new java.util.ArrayList<>(artifacts);
            state.megaProjects = new java.util.ArrayList<>(megaProjects);
            state.worldTime = worldTime;

            java.io.File file = new java.io.File("memory/world/world_state.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
            worldEvents.add(new WorldEvent("Mundo guardado automáticamente", worldTime, 0, 255, 100));
        } catch (Exception ex) {
            System.err.println("Error saving world state: " + ex.getMessage());
            worldEvents.add(new WorldEvent("Error guardando mundo", worldTime, 255, 0, 0));
        }
    }

    private void drawJettraStatusTooltip() {
        HumanEntity jettra = null;
        for (HumanEntity e : entities) {
            if (e.name.contains("Jettra")) { jettra = e; break; }
        }
        if (jettra == null) return;

        int x = 10;
        int y = 200;
        int w = 280;
        int h = 100;

        drawRectangleRounded(new Rectangle().x(x).y(y).width(w).height(h), 0.2f, 8, fade(DARKGRAY, 0.9f));
        drawRectangleRoundedLines(new Rectangle().x(x).y(y).width(w).height(h), 0.2f, 8, GOLD);
        
        drawText("VOZ DEL LÍDER: " + jettra.name.toUpperCase(), x + 10, y + 10, 12, GOLD);
        drawText("ACCIÓN: " + jettra.action, x + 10, y + 30, 10, RAYWHITE);
        drawText("Población Total: " + entities.size(), x + 10, y + 45, 10, SKYBLUE);
        drawText("Proyectos Activos: " + megaProjects.size(), x + 140, y + 45, 10, SKYBLUE);
        
        if (jettra.currentThought != null && jettra.thoughtTimer > 0) {
            String thought = jettra.currentThought;
            int maxChars = 40;
            if (thought.length() > maxChars) thought = thought.substring(0, maxChars-3) + "...";
            drawText("PENSANDO: " + thought, x + 10, y + 70, 11, LIME);
        } else {
            drawText("PENSANDO: Vigilando el conocimiento...", x + 10, y + 70, 11, GRAY);
        }
    }

    public static void main(String[] args) {
        new Jettra3DApp().run();
    }
}
