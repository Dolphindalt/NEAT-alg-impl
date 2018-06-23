BUILD_DIR := src/

make:
	javac $(BUILD_DIR)*.java

clean:
	rm $(BUILD_DIR)*.class