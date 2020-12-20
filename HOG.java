import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

public class HOG {
	private static final int NUM_BINS = 8;
	private static final int IMAGE_HEIGHT = 150;
	private static final int IMAGE_WIDTH = 150;
	private static final int CELL_HEIGHT = 10;
	private static final int CELL_WIDTH = 10;
	private static final int NUM_CELLS = IMAGE_HEIGHT * IMAGE_WIDTH / (CELL_HEIGHT * CELL_WIDTH);
	private static final String OUTPUT_PATH = "./output.txt";
	private static final String[] IMAGE_PATHS = { "./1.png", "./2.png", "./3.png", "./4.png", "./5.png", "./6.png" };

	public static void main(String[] args) {

		// initialize Wx, Wy, G, and theta
		int[][] Wx = { { -1, 0, 1 }, { -2, 0, 2 }, { -1, 0, 1 } };
		int[][] Wy = { { -1, -2, -1 }, { 0, 0, 0 }, { 1, 2, 1 } };
		Double[][] G = new Double[IMAGE_HEIGHT][IMAGE_WIDTH];
		Double[][] theta = new Double[IMAGE_HEIGHT][IMAGE_WIDTH];
		Double[][][] hogs = new Double[IMAGE_PATHS.length][NUM_CELLS][NUM_BINS];

		try {
			for (int i = 0; i < IMAGE_PATHS.length; i++) {
				int[][] image = readImage(IMAGE_PATHS[i]);
				constructHOGData(image, Wx, Wy, G, theta);
				Double[][] hog = getHOGs(G, theta);
				hogs[i] = hog;
			}
			writeFile(hogs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int[][] readImage(String path) throws IOException {
		BufferedImage bi = ImageIO.read(new File(path));
		int[][] out = new int[IMAGE_HEIGHT][IMAGE_WIDTH];
		for (int i = 0; i < IMAGE_HEIGHT; i++) {
			for (int j = 0; j < IMAGE_WIDTH; j++) {
				Color color = new Color(bi.getRGB(j, i));
				out[i][j] = (color.getRed() + color.getBlue() + color.getGreen()) / 3;
			}
		}
		return out;
	}

	// pass G and theta by reference and this method will fill in the values
	// Convolution formula simplified since HOG sees rotation by 180 degrees as
	// equal (no need to "flip" kernel)
	public static void constructHOGData(int[][] in, int[][] Wx, int[][] Wy, Double[][] G, Double[][] theta) {
		int[][] Gx = new int[IMAGE_HEIGHT][IMAGE_WIDTH];
		int[][] Gy = new int[IMAGE_HEIGHT][IMAGE_WIDTH];

		// zero pad
		int[][] inZP = new int[IMAGE_HEIGHT + 2][IMAGE_WIDTH + 2];
		for (int i = 0; i < IMAGE_HEIGHT; i++)
			for (int j = 0; j < IMAGE_WIDTH; j++)
				inZP[i + 1][j + 1] = in[i][j];

		for (int i = 0; i < IMAGE_HEIGHT; i++)
			for (int j = 0; j < IMAGE_WIDTH; j++) {
				for (int l = 0; l < Wx.length; l++)
					for (int m = 0; m < Wx[0].length; m++) {
						Gx[i][j] += inZP[i + l][j + m] * Wx[l][m];
						Gy[i][j] += inZP[i + l][j + m] * Wy[l][m];
					}
				G[i][j] = Math.sqrt(Gx[i][j] * Gx[i][j] + Gy[i][j] * Gy[i][j]);
				theta[i][j] = Math.atan2(Gx[i][j], Gy[i][j]); // using x/y so lines are tangent instead of normal
			}
	}

	// separates into regions, calculates bins, thetas (adding pi if < 0 )
	// [cell][bin]
	public static Double[][] getHOGs(Double[][] G, Double[][] theta) {
		// initialize hog
		Double[][] hog = new Double[NUM_CELLS][NUM_BINS];
		for (int i = 0; i < hog.length; i++)
			for (int j = 0; j < hog[0].length; j++)
				hog[i][j] = 0.0;

		// add pi to all thetas if less than 0
		for (int i = 0; i < theta.length; i++)
			for (int j = 0; j < theta[0].length; j++)
				if (theta[i][j] < 0)
					theta[i][j] += Math.PI;

		int numCellsX = IMAGE_WIDTH / CELL_WIDTH;
		int numCellsY = IMAGE_HEIGHT / CELL_HEIGHT;

		for (int cx = 0; cx < numCellsX; cx++)
			for (int cy = 0; cy < numCellsY; cy++)
				for (int j = cy * CELL_HEIGHT; j < (cy + 1) * CELL_HEIGHT; j++)
					for (int k = cx * CELL_WIDTH; k < (cx + 1) * CELL_WIDTH; k++)
						hog[numCellsX * cy + cx][(int) Math.floor(theta[j][k] / (Math.PI / NUM_BINS)) % NUM_BINS] += G[j][k];

		return hog;
	}

	public static void writeFile(Double[][][] hogs) throws IOException, FileNotFoundException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH));
		for (int im = 0; im < IMAGE_PATHS.length; im++) {
			for (int i = 0; i < hogs[im].length; i++)
				for (int j = 0; j < hogs[im][0].length; j++) {
					bw.write("" + hogs[im][i][j]);
					if (!(i == hogs[im].length - 1 && j == hogs[im][0].length - 1))
						bw.write(",");
					;
				}
			bw.newLine();
		}
		bw.close();
	}

}
