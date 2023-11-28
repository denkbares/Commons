package com.denkbares.llm.instructor;

import java.nio.file.Path;
import java.nio.file.Paths;

class JavaExampleInstructor {

	public static void main(String[] args) {
		//you can download the folder INSTRUCTOR-XL from "https://cloud.office.denkbares.com/apps/files/?dir=/Storage/Projekte/Troi-LLM" (5GB!)

		//specify path to modelfile via the .onnx file located in the downloaded folder
		Path modelFile = Paths.get("<YOUR_PATH_TO>/INSTRUCTOR-XL/outnnx.onnx");

		//load the instructor
		InstructorTransformer instructor = InstructorTransformer.Companion.fromPath(modelFile);

		//specify your input:
		InstructorInput input = new InstructorInput(
				"Create an embedding of this description for retrieval",
				"The quick brown fox jumps over the lazy dog"
		);

		//and get your embedding
		float[] embedding = instructor.embed(input);
	}
}