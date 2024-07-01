package org.example;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    public enum Movement {
        FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN
    }

    private Vector3f position;
    private Vector3f front;
    private Vector3f up;
    private Vector3f right;
    private Vector3f worldUp;
    private float yaw;
    private float pitch;
    private float mouseSensitivity;

    public Camera() {
        position = new Vector3f(0.0f, 1.0f, 3.0f);
        front = new Vector3f(0.0f, 0.0f, -1.0f);
        up = new Vector3f(0.0f, 1.0f, 0.0f);
        right = new Vector3f(1.0f, 0.0f, 0.0f);
        worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        yaw = -90.0f;
        pitch = 0.0f;
        mouseSensitivity = 0.1f;
        updateCameraVectors();
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(position, position.add(front, new Vector3f()), up);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public void processKeyboard(Movement direction, float deltaTime) {
        float velocity = deltaTime * 2.5f;
        if (direction == Movement.FORWARD) {
            position.add(new Vector3f(front).mul(velocity));
        }
        if (direction == Movement.BACKWARD) {
            position.sub(new Vector3f(front).mul(velocity));
        }
        if (direction == Movement.LEFT) {
            position.sub(new Vector3f(right).mul(velocity));
        }
        if (direction == Movement.RIGHT) {
            position.add(new Vector3f(right).mul(velocity));
        }
        if (direction == Movement.UP) {
            position.add(new Vector3f(worldUp).mul(velocity));
        }
        if (direction == Movement.DOWN) {
            position.sub(new Vector3f(worldUp).mul(velocity));
        }
    }

    public void processMouseMovement(float xOffset, float yOffset) {
        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;

        yaw += xOffset;
        pitch += yOffset;

        // Make sure that when pitch is out of bounds, screen doesn't get flipped
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        // Update Front, Right and Up Vectors using the updated Euler angles
        updateCameraVectors();
    }

    private void updateCameraVectors() {
        // Calculate the new Front vector
        Vector3f front = new Vector3f();
        front.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        this.front.set(front.normalize());

        // Also re-calculate the Right and Up vector
        right.set(front.cross(worldUp, new Vector3f()).normalize());  // Normalize the vectors, because their length gets closer to 0 the more you look up or down which results in slower movement.
        up.set(right.cross(front, new Vector3f()).normalize());
    }
}
