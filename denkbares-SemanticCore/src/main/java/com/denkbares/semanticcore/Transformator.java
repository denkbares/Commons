package com.denkbares.semanticcore;

public interface Transformator {

	void transform(String targetFile, SemanticCore core);

	/**
	 * Returns true if the transformator exports exactly one file. If the
	 * transformator exports a couple of files, than this method returns false.
	 * <p>
	 * This information is necessary to determine whether a File- or
	 * Directory-Chosing dialog is presented in the ReviewTool.
	 *
	 * @return true, if the transformator exports exactly one file.
	 */
	boolean isFileTransformator();

	String getTitle();

}
