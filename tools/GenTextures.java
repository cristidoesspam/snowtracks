import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class GenTextures {
	static final int N = 32;
	static final int SS = 4;

	interface Shape {
		boolean in(double x, double y);
	}

	static boolean ell(double x, double y, double cx, double cy, double rx, double ry) {
		double a = (x - cx) / rx;
		double b = (y - cy) / ry;
		return a * a + b * b <= 1;
	}

	static boolean seg(double x, double y, double x0, double y0, double x1, double y1, double w0, double w1) {
		double dx = x1 - x0;
		double dy = y1 - y0;
		double t = Math.max(0, Math.min(1, ((x - x0) * dx + (y - y0) * dy) / (dx * dx + dy * dy)));
		double px = x0 + dx * t;
		double py = y0 + dy * t;
		double w = w0 + (w1 - w0) * t;
		return Math.hypot(x - px, y - py) <= w;
	}

	static final Shape BOOT = (x, y) -> {
		int px = (int) Math.floor((x + 1) * N / 2);
		int py = (int) Math.floor((y + 1) * N / 2);
		return px >= 11 && px <= 20 && (py >= 4 && py <= 13 || py >= 18 && py <= 27);
	};

	static final Shape PAW = (x, y) -> ell(x, y, 0, 0.30, 0.36, 0.30)
		|| ell(x, y, -0.55, -0.12, 0.15, 0.22) || ell(x, y, -0.20, -0.48, 0.15, 0.22)
		|| ell(x, y, 0.20, -0.48, 0.15, 0.22) || ell(x, y, 0.55, -0.12, 0.15, 0.22);

	static final Shape HOOF = (x, y) -> {
		double ax = Math.abs(x);
		return ax >= 0.07 && ell(ax, y, 0.33 - (y < 0 ? 0.12 * -y : 0), 0.05, 0.24, 0.60);
	};

	static final Shape BIRD = (x, y) -> seg(x, y, 0, 0.1, -0.62, -0.78, 0.12, 0.05) || seg(x, y, 0, 0.1, 0, -0.92, 0.12, 0.05)
		|| seg(x, y, 0, 0.1, 0.62, -0.78, 0.12, 0.05) || seg(x, y, 0, 0.1, 0.05, 0.78, 0.09, 0.04) || ell(x, y, 0, 0.12, 0.17, 0.17);

	static final Shape RABBIT = (x, y) -> ell(x, y, -0.34, -0.42, 0.20, 0.44) || ell(x, y, 0.34, -0.42, 0.20, 0.44)
		|| ell(x, y, -0.12, 0.50, 0.13, 0.15) || ell(x, y, 0.14, 0.80, 0.13, 0.15);

	static final Shape DRAG = (x, y) -> Math.abs(x) < 0.46 + 0.05 * Math.sin(y * Math.PI * 3);

	static float[][] coverage(Shape shape) {
		float[][] cov = new float[N][N];
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < N; i++) {
				int hit = 0;
				for (int sj = 0; sj < SS; sj++) {
					for (int si = 0; si < SS; si++) {
						double x = (i + (si + 0.5) / SS) / N * 2 - 1;
						double y = (j + (sj + 0.5) / SS) / N * 2 - 1;
						if (shape.in(x, y)) {
							hit++;
						}
					}
				}
				cov[i][j] = hit / (float) (SS * SS);
			}
		}
		return cov;
	}

	static float[][] distance(float[][] cov, boolean inside) {
		float[][] d = new float[N][N];
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < N; i++) {
				if ((cov[i][j] >= 0.5f) != inside) {
					continue;
				}
				double best = 99;
				for (int l = 0; l < N; l++) {
					for (int k = 0; k < N; k++) {
						if ((cov[k][l] >= 0.5f) != inside) {
							best = Math.min(best, Math.hypot(i - k, j - l));
						}
					}
				}
				d[i][j] = (float) best;
			}
		}
		return d;
	}

	static float[][] blur(float[][] a, double sigma) {
		int r = (int) Math.ceil(sigma * 3);
		float[][] out = new float[N][N];
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < N; i++) {
				double sum = 0;
				double weights = 0;
				for (int l = -r; l <= r; l++) {
					for (int k = -r; k <= r; k++) {
						double w = Math.exp(-(k * k + l * l) / (2 * sigma * sigma));
						int x = Math.max(0, Math.min(N - 1, i + k));
						int y = Math.max(0, Math.min(N - 1, j + l));
						sum += a[x][y] * w;
						weights += w;
					}
				}
				out[i][j] = (float) (sum / weights);
			}
		}
		return out;
	}

	static BufferedImage render(Shape shape, String style) {
		float[][] cov = coverage(shape);
		float[][] in = distance(cov, true);
		float[][] out = distance(cov, false);
		float[][] soft = blur(cov, 1.5);
		BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
		for (int j = 0; j < N; j++) {
			for (int i = 0; i < N; i++) {
				float shade = 0;
				float alpha = 0;
				float c = cov[i][j];
				switch (style) {
					case "crisp" -> {
						if (c > 0) {
							boolean lip = in[i][j] < 1.6f;
							shade = lip ? 0.80f : 0.60f;
							alpha = (lip ? 0.62f : 0.92f) * c;
						}
					}
					case "soft" -> {
						shade = 0.68f;
						alpha = 0.8f * Math.min(1f, soft[i][j] * 1.3f);
					}
					case "wet" -> {
						if (c > 0) {
							shade = 0.22f;
							alpha = 0.9f * c;
						}
						float d = out[i][j];
						if (c < 1 && d > 0 && d <= 2.4f) {
							float ring = 0.7f * (1 - d / 2.6f) * (1 - c);
							if (ring > alpha) {
								shade = 1f;
								alpha = ring;
							}
						}
					}
					default -> throw new IllegalArgumentException(style);
				}
				int g = Math.round(shade * 255);
				int a = Math.round(Math.max(0, Math.min(1, alpha)) * 255);
				img.setRGB(i, j, a << 24 | g << 16 | g << 8 | g);
			}
		}
		return img;
	}

	static void icon(File file) throws Exception {
		int size = 128;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double t = py / (double) size;
				double r = 214 + 30 * (1 - t);
				double g = 230 + 20 * (1 - t);
				double b = 248 + 7 * (1 - t);
				double hit = 0;
				for (int s = 0; s < 9; s++) {
					double x = px + (s % 3 + 0.5) / 3;
					double y = py + (s / 3 + 0.5) / 3;
					if (inPrint(BOOT, x, y, 38, 92, 60, 0) || inPrint(BOOT, x, y, 88, 40, 60, 0)) {
						hit++;
					}
				}
				double a = 0.85 * hit / 9;
				r = r * (1 - a) + 96 * a;
				g = g * (1 - a) + 124 * a;
				b = b * (1 - a) + 158 * a;
				img.setRGB(px, py, 0xFF000000 | (int) r << 16 | (int) g << 8 | (int) b);
			}
		}
		ImageIO.write(img, "png", file);
	}

	static boolean inPrint(Shape s, double x, double y, double cx, double cy, double size, double angle) {
		double dx = (x - cx) / (size / 2);
		double dy = (y - cy) / (size / 2);
		double c = Math.cos(angle);
		double sn = Math.sin(angle);
		return s.in(dx * c + dy * sn, -dx * sn + dy * c);
	}

	public static void main(String[] args) throws Exception {
		File root = new File(args[0]);
		File dir = new File(root, "textures/particle");
		dir.mkdirs();
		String[] names = {"boot", "paw", "hoof", "bird", "rabbit", "drag"};
		Shape[] shapes = {BOOT, PAW, HOOF, BIRD, RABBIT, DRAG};
		for (int i = 0; i < names.length; i++) {
			for (String style : new String[] {"crisp", "soft", "wet"}) {
				ImageIO.write(render(shapes[i], style), "png", new File(dir, names[i] + "_" + style + ".png"));
			}
		}
		icon(new File(root, "icon.png"));
	}
}
