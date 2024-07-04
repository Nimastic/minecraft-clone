##Prerequisites
Before you start, make sure you have the following installed on your system:

Java Development Kit (JDK) 8 or later
Git
Gradle

##Project Structure

```
minecraft-clone/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── org/
│   │   │   │   │   ├── example/
│   │   │   │   │   │   ├── App.java
│   │   │   │   │   │   ├── Camera.java
│   │   │   │   │   │   ├── ShaderUtils.java
│   │   │   │   ├── FastNoiseLite/
│   │   │   │   │   ├── FastNoiseLite.java
│   │   │   ├── resources/
│   │   │   │   ├── shaders/
│   │   │   │   │   ├── vertex_shader.glsl
│   │   │   │   │   ├── fragment_shader.glsl
│   │   │   │   ├── textures/
│   │   │   │   │   ├── grass_block_side.png
│   │   │   │   │   ├── grass_block_top.png
│   │   │   │   │   ├── grass_block_bottom.png
├── build.gradle.kts
├── settings.gradle.kts
```

```
./gradlew build
./gradlew run
```
