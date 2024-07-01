package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import FastNoiseLite.FastNoiseLite;

public class App {
    private long window;
    private int shaderProgram;
    private int vao;
    private Camera camera;
    private int windowWidth = 800;
    private int windowHeight = 600;
    private boolean firstMouse = true;
    private float lastX = windowWidth / 2.0f;
    private float lastY = windowHeight / 2.0f;
    private List<Vector3f> blocks = new ArrayList<>();
    private float cameraSpeed = 0.05f;

    private int crosshairVao;

    private void setupCrosshair() {
        float[] vertices = {
            -0.01f,  0.0f, 0.0f,
             0.01f,  0.0f, 0.0f,
             0.0f, -0.01f, 0.0f,
             0.0f,  0.01f, 0.0f,
        };
    
        crosshairVao = GL30.glGenVertexArrays();
        int vbo = GL20.glGenBuffers();
    
        GL30.glBindVertexArray(crosshairVao);
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, vertexBuffer, GL20.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        MemoryUtil.memFree(vertexBuffer);
    }
    
    private void renderCrosshair() {
        GL30.glBindVertexArray(crosshairVao);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 4);
        GL30.glBindVertexArray(0);
    }


    public void run() {
        init();
        loop();

        // Free the window callbacks and destroy the window
        GLFW.glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Set up an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // Create the window
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight, "3D Cube", 0, 0);
        if (window == 0L) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        // Enable v-sync
        GLFW.glfwSwapInterval(1);

        // Make the window visible
        GLFW.glfwShowWindow(window);

        // Initialize GL capabilities
        GL.createCapabilities();

        // Set up the shaders
        setupShaders();

        // Set up the vertex data
        setupVertexData();

        // Initialize camera
        camera = new Camera();

        // Initialize crosshair
        setupCrosshair();

        // Set the framebuffer size callback
        GLFW.glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                windowWidth = width;
                windowHeight = height;
                GL11.glViewport(0, 0, width, height);
            }
        });

        // Set the cursor position callback
        GLFW.glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                if (firstMouse) {
                    lastX = (float) xpos;
                    lastY = (float) ypos;
                    firstMouse = false;
                }

                float xOffset = (float) xpos - lastX;
                float yOffset = lastY - (float) ypos; // Reversed since y-coordinates go from bottom to top

                lastX = (float) xpos;
                lastY = (float) ypos;

                camera.processMouseMovement(xOffset, yOffset);
            }
        });

        // Set the mouse button callback
        GLFW.glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (action == GLFW.GLFW_PRESS) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        addBlock();
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        removeBlock();
                    }
                }
            }
        });

        // Capture the mouse
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        // Set the initial viewport
        GL11.glViewport(0, 0, windowWidth, windowHeight);

        // Generate the chunk
        generateChunk();
    }

    private void setupShaders() {
        // Load shader source code from files
        String vertexShaderSource = ShaderUtils.loadShaderSource("src/main/resources/shaders/vertex_shader.glsl");
        String fragmentShaderSource = ShaderUtils.loadShaderSource("src/main/resources/shaders/fragment_shader.glsl");

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);
        checkCompileErrors(vertexShader, "VERTEX");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);
        checkCompileErrors(fragmentShader, "FRAGMENT");

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        checkCompileErrors(shaderProgram, "PROGRAM");

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void checkCompileErrors(int shader, String type) {
        int success;
        if (type.equals("PROGRAM")) {
            success = GL20.glGetProgrami(shader, GL20.GL_LINK_STATUS);
            if (success == GL11.GL_FALSE) {
                String infoLog = GL20.glGetProgramInfoLog(shader);
                System.out.println("ERROR::PROGRAM_LINKING_ERROR of type: " + type + "\n" + infoLog);
            }
        } else {
            success = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
            if (success == GL11.GL_FALSE) {
                String infoLog = GL20.glGetShaderInfoLog(shader);
                System.out.println("ERROR::SHADER_COMPILATION_ERROR of type: " + type + "\n" + infoLog);
            }
        }
    }

    private void setupVertexData() {
        // Define vertices for a cube
        float[] vertices = generateCubeVertices();

        vao = GL30.glGenVertexArrays();
        int vbo = GL20.glGenBuffers();

        GL30.glBindVertexArray(vao);

        // Bind VBO
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        // Transfer vertex data to VBO
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, vertexBuffer, GL20.GL_STATIC_DRAW);

        // Define vertex attributes
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 6 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 6 * 4, 3 * 4);
        GL20.glEnableVertexAttribArray(1);

        // Unbind VBO and VAO
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // Free the allocated memory
        MemoryUtil.memFree(vertexBuffer);
    }

    private float[] generateCubeVertices() {
        return new float[]{
            // positions          // colors
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // green
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  // blue
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  // blue
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f,  // yellow
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red

            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  // magenta
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,  // cyan
             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink
             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink
            -0.5f,  0.5f,  0.5f,  0.5f, 1.0f, 0.5f,  // light green
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  // magenta

            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,  // green
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f,  // yellow
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  // magenta
            -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 0.0f,  // green

             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  // blue
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // green
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // green
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,  // cyan
             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink

            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red
             0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // green
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,  // cyan
             0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,  // cyan
            -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,  // magenta
            -0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 0.0f,  // red

            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f,  // yellow
             0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,  // blue
             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink
             0.5f,  0.5f,  0.5f,  1.0f, 0.5f, 0.5f,  // pink
            -0.5f,  0.5f,  0.5f,  0.5f, 1.0f, 0.5f,  // light green
            -0.5f,  0.5f, -0.5f,  1.0f, 1.0f, 0.0f   // yellow
        };
    }

    private void loop() {
        // Set the clear color
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Enable depth testing
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        while (!GLFW.glfwWindowShouldClose(window)) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // Clear the framebuffer

            // Handle input
            handleInput();

            // Use the shader program
            GL20.glUseProgram(shaderProgram);

            // Set the view and projection matrices
            int modelLoc = GL20.glGetUniformLocation(shaderProgram, "model");
            int viewLoc = GL20.glGetUniformLocation(shaderProgram, "view");
            int projLoc = GL20.glGetUniformLocation(shaderProgram, "projection");

            // Matrix4f model = new Matrix4f().identity();
            // GL20.glUniformMatrix4fv(modelLoc, false, model.get(new float[16]));

            Matrix4f view = camera.getViewMatrix();
            GL20.glUniformMatrix4fv(viewLoc, false, view.get(new float[16]));

            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f), (float) windowWidth / (float) windowHeight, 0.1f, 100.0f);
            GL20.glUniformMatrix4fv(projLoc, false, projection.get(new float[16]));

            // Render each block
            for (Vector3f block : blocks) {
                Matrix4f model = new Matrix4f().translate(block);
                GL20.glUniformMatrix4fv(modelLoc, false, model.get(new float[16]));

                // Bind the VAO
                GL30.glBindVertexArray(vao);
                // Draw the cube
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36); // 36 vertices for a cube (6 faces * 2 triangles * 3 vertices)
                // Unbind the VAO
                GL30.glBindVertexArray(0);
            }

            // Render the crosshair
            renderCrosshair();

            // Swap the color buffers
            GLFW.glfwSwapBuffers(window);

            // Poll for window events
            GLFW.glfwPollEvents();
        }
    }

    private void handleInput() {
        float cameraSpeed = 0.05f;
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.FORWARD, cameraSpeed);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.BACKWARD, cameraSpeed);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.LEFT, cameraSpeed);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.RIGHT, cameraSpeed);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.UP, cameraSpeed);
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            camera.processKeyboard(Camera.Movement.DOWN, cameraSpeed);
        }
    }

    private void generateChunk() {
        FastNoiseLite noise = new FastNoiseLite();
        noise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        noise.SetFrequency(0.1f);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int height = (int) (noise.GetNoise(x, z) * 8 + 8); // Generate height value between 0 and 16
                for (int y = 0; y < height; y++) {
                    blocks.add(new Vector3f(x, y, z));
                }
            }
        }
    }

    private Vector3f rayCast() {
        float step = 0.1f;
        float maxDist = 5.0f;
        Vector3f rayOrigin = camera.getPosition();
        Vector3f rayDir = camera.getFront().normalize();
        System.out.println("Ray Origin: " + rayOrigin + " Ray Direction: " + rayDir);
    
        for (float t = 0; t < maxDist; t += step) {
            Vector3f pos = new Vector3f(rayOrigin).add(rayDir.mul(t, new Vector3f()), new Vector3f());
            Vector3f blockPos = new Vector3f(Math.round(pos.x), Math.round(pos.y), Math.round(pos.z));
            for (Vector3f block : blocks) {
                if (block.equals(blockPos)) {
                    System.out.println("Hit block at: " + blockPos);
                    return block;
                }
            }
        }
        System.out.println("No block hit");
        return null;
    }
    
    private void addBlock() {
        Vector3f block = rayCast();
        if (block != null) {
            Vector3f normal = getBlockFaceNormal(block);
            if (normal != null) {
                Vector3f newBlock = new Vector3f(block).add(normal);
                if (!blocks.contains(newBlock)) {
                    blocks.add(newBlock);
                    System.out.println("Added block at: " + newBlock);
                }
            }
        }
    }
    
    private void removeBlock() {
        Vector3f block = rayCast();
        if (block != null) {
            blocks.remove(block);
            System.out.println("Removed block at: " + block);
        }
    }
    
    private Vector3f getBlockFaceNormal(Vector3f block) {
        Vector3f rayOrigin = camera.getPosition();
        Vector3f rayDirection = camera.getFront().normalize();
    
        Vector3f[] faces = {
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
            new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
        };
    
        for (Vector3f face : faces) {
            Vector3f planePoint = new Vector3f(block).add(face);
            Vector3f planeNormal = new Vector3f(face);
            float distance = planePoint.sub(rayOrigin, new Vector3f()).dot(planeNormal) / rayDirection.dot(planeNormal);
    
            if (distance > 0 && distance < 5.0f) {
                Vector3f intersection = new Vector3f(rayOrigin).add(rayDirection.mul(distance, new Vector3f()), new Vector3f());
                if (Math.abs(intersection.x - planePoint.x) < 0.5 && Math.abs(intersection.y - planePoint.y) < 0.5 && Math.abs(intersection.z - planePoint.z) < 0.5) {
                    System.out.println("Face normal: " + face);
                    return face;
                }
            }
        }
        System.out.println("No face normal found");
        return null;
    }
    

    public static void main(String[] args) {
        new App().run();
    }
}
