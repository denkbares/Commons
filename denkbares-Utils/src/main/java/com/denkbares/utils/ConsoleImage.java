/*
 * Copyright (C) 2024 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.jetbrains.annotations.Nullable;

import com.denkbares.utils.Consoles.RGBColor;
import com.denkbares.utils.Consoles.Style;

import static com.denkbares.utils.Consoles.NOP;
import static com.denkbares.utils.Consoles.RESET;

/**
 * Utility class to preview an image on an ascii text console.
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 04.02.2024
 */
public class ConsoleImage {
	/**
	 * the maximum width (in ascii characters) the image should be displayed at.
	 */
	private int maxWidth = 80;
	/**
	 * the aspect-ratio of a single ascii character of the text console. By default, the aspect ratio is assumed to be
	 * 5:2. This is roughly correct for most terminal emulations.
	 */
	private float charAspectRatio = 0.4f;
	/**
	 * Determines if console colors should be used, or if the image should be displayed in plain ascii.
	 */
	private boolean useColors = true;

	/**
	 * Sets the maximum width (in ascii characters) the image should be displayed at, and returns this console image for
	 * further processing.
	 */
	public ConsoleImage maxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
		return this;
	}

	/**
	 * Sets the aspect-ratio of a single ascii character of the text console, and returns this console image for further
	 * processing. By default, the aspect ratio is assumed to be 5:2. This is roughly correct for most terminal
	 * emulations.
	 */
	public ConsoleImage charAspectRatio(float charAspectRatio) {
		this.charAspectRatio = charAspectRatio;
		return this;
	}

	/**
	 * Sets if console colors should be used, or if the image should be displayed in plain ascii. It returns this
	 * console image for further processing.
	 */
	public ConsoleImage useColors(boolean useColors) {
		this.useColors = useColors;
		return this;
	}

	/**
	 * Loads the image from the specified source and displays it on the console, using the previously applied setting.
	 * It supports all file formats according to {@link ImageIO}.
	 *
	 * @param bytes the image data to be loaded and displayed
	 * @throws IOException if the data does not provide valid (supported) image information
	 */
	public void println(byte[] bytes) throws IOException {
		println(new ByteArrayInputStream(bytes));
	}

	/**
	 * Loads the image from the specified source and displays it on the console, using the previously applied setting.
	 * It supports all file formats according to {@link ImageIO}.
	 *
	 * @param imageData the image data to be loaded and displayed
	 * @throws IOException if the stream could not be read or does not provide valid (supported) image information
	 */
	public void println(InputStream imageData) throws IOException {
		var image = ImageIO.read(imageData);
		if (image == null) throw new IOException("unsupported image format");
		println(image);
	}

	/**
	 * Loads the image from the specified source and displays it on the console, using the previously applied setting.
	 * It supports all file formats according to {@link ImageIO}.
	 *
	 * @param imageFile the image data to be loaded and displayed
	 * @throws IOException if the file could not be read or does not provide valid (supported) image information
	 */
	public void println(File imageFile) throws IOException {
		var image = ImageIO.read(imageFile);
		if (image == null) throw new IOException("unsupported image format: " + imageFile);
		println(image);
	}

	/**
	 * Loads the image from the specified source and displays it on the console, using the previously applied setting.
	 * It supports all file formats according to {@link ImageIO}.
	 *
	 * @param imageURL the image data to be loaded and displayed
	 * @throws IOException if the URL could not be read or does not provide valid (supported) image information
	 */
	public void println(URL imageURL) throws IOException {
		var image = ImageIO.read(imageURL);
		if (image == null) throw new IOException("unsupported image format: " + imageURL);
		println(image);
	}

	/**
	 * Displays the specified image on the console, using the previously applied setting.
	 *
	 * @param image the image to be displayed
	 */
	public void println(BufferedImage image) {
		// resize the image to fit the maximum width
		var resizedImage = fitImage(image);
		var back1 = new Color(1.0f, 0.6f, 0.6f, 0.6f);
		var back2 = new Color(1.0f, 0.5f, 0.5f, 0.5f);
		int square = Math.max(1, Math.round(3 / charAspectRatio));

		// print image as colored ascii
		for (int y = 0; y < resizedImage.getHeight(); y += 2) {
			for (int x = 0; x < resizedImage.getWidth(); x++) {
				var back = (!useColors) ? null : ((x / square + y / 6) % 2 == 0) ? back1 : back2;
				var upper = withBackground(color(resizedImage, x, y), back);
				var lower = withBackground(color(resizedImage, x, y + 1), back);
				var above = withBackground(color(resizedImage, x, y - 1), back);
				var below = withBackground(color(resizedImage, x, y + 2), back);
				printPixel(upper, lower, above, below);
			}
			// new line + reset ANSI Code
			System.out.println();
		}
	}

	private void printPixel(@Nullable Color upper, @Nullable Color lower, @Nullable Color above, @Nullable Color below) {
		if (useColors) {
			// we can use either UPPER HALF BLOCK or LOWER HALF BLOCK.
			// as there might be line-spacing visible (above/below the line), showing the background,
			// we use that char so that the background produces less noise between the adjacent lines
			if (difference(upper, above) < difference(lower, below)) {
				// similarity above is closer, so use background color as upper pixel
				// use the LOWER HALF BLOCK ascii character -->
				// background color is upper pixel, foreground color is lower pixel
				System.out.print(toConsoleColor(upper, true).getCode());
				System.out.print(toConsoleColor(lower, false).getCode());
				System.out.print("▄");
				System.out.print(RESET.getCode());
			}
			else {
				// similarity below is closer, so use background color as upper pixel
				// use the UPPER HALF BLOCK ascii character -->
				// foreground color is upper pixel, background color is lower pixel
				System.out.print(toConsoleColor(upper, false).getCode());
				System.out.print(toConsoleColor(lower, true).getCode());
				System.out.print("▀");
				System.out.print(RESET.getCode());
			}
		}
		else {
			var palette = " .,:;+*?%S#@"; // inverse luminescence order, as consoles are usually in dark mode
			var luminance = (luminance(upper) + luminance(lower)) / 2.0;
			System.out.print(palette.charAt(Math.min((int) (luminance * palette.length()), palette.length() - 1)));
		}
	}

	/**
	 * Calculates the difference between two colors, as a value between 0.0 and 1.0. For missing colors, the difference
	 * is below 0.
	 */
	private static double difference(@Nullable Color p1, @Nullable Color p2) {
		if (p1 == null || p2 == null) return -1.0; // better than no difference
		var r1 = p1.red() / 255.0;
		var r2 = p2.red() / 255.0;
		var g1 = p1.green() / 255.0;
		var g2 = p2.green() / 255.0;
		var b1 = p1.blue() / 255.0;
		var b2 = p2.blue() / 255.0;
		return Math.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2));
	}

	private BufferedImage fitImage(BufferedImage originalImage) {
		// calculate scale; if it already fits, use original image
		var scale = maxWidth / (float) originalImage.getWidth();
		if (scale > 1) return originalImage;

		// otherwise scale to fit width
		return scaleImage(originalImage, scale, scale * charAspectRatio * 2);
	}

	private BufferedImage scaleImage(BufferedImage source, float scaleX, float scaleY) {
		var width = Math.round(source.getWidth() * scaleX);
		var height = Math.round(source.getHeight() * scaleY);
		var resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < height; y++) {
			var y1 = y / scaleY;
			var y2 = (y + 1) / scaleY;
			for (int x = 0; x < width; x++) {
				var x1 = x / scaleX;
				var x2 = (x + 1) / scaleX;
				var color = interpolate(source, x1, x2, y1, y2);
				resized.setRGB(x, y, color.rgb);
			}
		}

		return resized;
	}

	private static float weightOfPixel(float min, int start, int pos, int end, float max) {
		if (start == end) return Math.abs(max - min);
		if (pos == start) return Math.abs(min - pos);
		if (pos == end) return 1 - Math.abs(pos - max);
		return 1f;
	}

	private static Color interpolate(BufferedImage source, float x1, float x2, float y1, float y2) {
		var xstart = (int) Math.floor(x1);
		var xend = (int) Math.ceil(x2);
		var ystart = (int) Math.floor(y1);
		var yend = (int) Math.ceil(y2);

		var total = 0f;
		var alpha = 0f;
		var red = 0f;
		var green = 0f;
		var blue = 0f;

		int ymax = Math.min(yend, source.getHeight() - 1);
		int xmax = Math.min(xend, source.getWidth() - 1);

		for (int y = ystart; y < ymax; y++) {
			var yw = weightOfPixel(y1, ystart, y, yend, y2);
			for (int x = xstart; x < xmax; x++) {
				var xw = weightOfPixel(x1, xstart, x, xend, x2);
				var weight = xw * yw;
				var color = new Color(source.getRGB(x, y));
				var opacity = color.alpha() / 255f;
				total += weight;
				alpha += opacity * weight;
				red += color.red() * opacity * weight / 255f;
				green += color.green() * opacity * weight / 255f;
				blue += color.blue() * opacity * weight / 255f;
			}
		}

		if (alpha < 0.001) return new Color(0f, 0f, 0f, 0f);
		return new Color(alpha / total, red / alpha, green / alpha, blue / alpha);
	}

	private static class Color {
		private final int rgb;

		public Color(int rgb) {
			this.rgb = rgb;
		}

		public Color(float alpha, float red, float green, float blue) {
			this.rgb =
					(Math.round(alpha * 255) << 24) +
							(Math.round(red * 255) << 16) +
							(Math.round(green * 255) << 8) +
							Math.round(blue * 255);
		}

		public int alpha() {
			return (rgb >> 24) & 0xff;
		}

		public int red() {
			return (rgb >> 16) & 0xff;
		}

		public int green() {
			return (rgb >> 8) & 0xff;
		}

		public int blue() {
			return rgb & 0xff;
		}
	}

	/**
	 * Returns the luminance of the color as a value between 0.0 and 1.0 (inclusively). The luminance also considers the
	 * alpha value, where transparent colors have zero luminance.
	 */
	private static double luminance(@Nullable Color color) {
		return (color == null) ? 0.0 : color.alpha() / 255.0 * (0.299 * color.red() + 0.587 * color.green() + 0.114 * color.blue()) / 255.0;
	}

	private static Style toConsoleColor(@Nullable Color color, boolean back) {
		if (color == null) return NOP;
		var rgb = new RGBColor(color.red(), color.green(), color.blue());
		return (back) ? rgb.toBackColor() : rgb;
	}

	private static @Nullable Color withBackground(@Nullable Color fore, @Nullable Color back) {
		// keep missing pixels as missing
		if (fore == null) return null;
		// if no background is specified, keep the color unchanged
		if (back == null) return fore;

		// otherwise mit the back with 1-alpha into the color
		var a1 = fore.alpha() / 255f;
		var a2 = (1f - a1);
		var r = fore.red() * a1 + back.red() * a2;
		var g = fore.green() * a1 + back.green() * a2;
		var b = fore.blue() * a1 + back.blue() * a2;
		return new Color(1f, r / 255f, g / 255f, b / 255f);
	}

	private static @Nullable Color color(BufferedImage img, int x, int y) {
		if (y < 0 || y >= img.getHeight() || x < 0 || x >= img.getWidth()) return null;
		return new Color(img.getRGB(x, y));
	}
}
