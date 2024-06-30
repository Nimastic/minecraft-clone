package org.example;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

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
        window = GLFW.glfwCreateWindow(windowWidth, windowHeight, "3D Flat Land", 0, 0);
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

        // Capture the mouse
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

        // Set the initial viewport
        GL11.glViewport(0, 0, windowWidth, windowHeight);
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
        // Define vertices for a flat land (grid of quads)
        float[] vertices = generateFlatLandVertices(10, 10);

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
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0);
        GL20.glEnableVertexAttribArray(0);

        // Unbind VBO and VAO
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

        // Free the allocated memory
        MemoryUtil.memFree(vertexBuffer);
    }

    private float[] generateFlatLandVertices(int width, int height) {
        int numVertices = width * height * 6; // 6 vertices per quad (2 triangles)
        float[] vertices = new float[numVertices * 3]; // 3 coordinates per vertex

        int index = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                float x0 = x;
                float x1 = x + 1;
                float z0 = z;
                float z1 = z + 1;

                // First triangle
                vertices[index++] = x0;
                vertices[index++] = 0.0f;
                vertices[index++] = z0;

                vertices[index++] = x1;
                vertices[index++] = 0.0f;
                vertices[index++] = z0;

                vertices[index++] = x0;
                vertices[index++] = 0.0f;
                vertices[index++] = z1;

                // Second triangle
                vertices[index++] = x1;
                vertices[index++] = 0.0f;
                vertices[index++] = z0;

                vertices[index++] = x1;
                vertices[index++] = 0.0f;
                vertices[index++] = z1;

                vertices[index++] = x0;
                vertices[index++] = 0.0f;
                vertices[index++] = z1;
            }
        }

        return vertices;
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

            Matrix4f model = new Matrix4f().identity();
            GL20.glUniformMatrix4fv(modelLoc, false, model.get(new float[16]));

            Matrix4f view = camera.getViewMatrix();
            GL20.glUniformMatrix4fv(viewLoc, false, view.get(new float[16]));

            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45.0f), (float) windowWidth / (float) windowHeight, 0.1f, 100.0f);
            GL20.glUniformMatrix4fv(projLoc, false, projection.get(new float[16]));

            // Bind the VAO
            GL30.glBindVertexArray(vao);
            // Draw the flat land
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6 * 10 * 10); // 6 vertices per quad, 10x10 grid
            // Unbind the VAO
            GL30.glBindVertexArray(0);

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

    public static void main(String[] args) {
        new App().run();
    }
}
