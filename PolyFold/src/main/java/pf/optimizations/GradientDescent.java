package pf.optimizations;

import pf.coreutils.Scoring;
import static pf.geometry.LinearAlgebra.*;
import pf.representations.*;

public class GradientDescent {
  public static Cartesian[] carts;
  public static double stepSize = 0.004;
  public static int iterations = 100000;
  public static final int MIN_ITERS = 50000;
  public static final int MAX_ITERS = 550000;
  public static final double MIN_STEP = 0;
  public static final double MAX_STEP = 0.05;

  public static Cartesian[] getNextState(Residue[] residues) {
    carts = Converter.residuesToCarts(residues);
    for (int i = 0; i < carts.length; i++) {
      double[] gradient = {0, 0, 0};
      for (int j = i+1; j < carts.length; j++) {
        double[] prev = gradient;
        gradient = getGradient(i, j);
        gradient = vectorAdd(prev, gradient);
        applyGradient(j, gradient);
      }
    }
    normalizeCarts();
    return carts;
  }

  public static void normalizeCarts() {
    for (int i = 0; i < carts.length-1; i++) {
      Point unscaled = pointSubtract(carts[i+1].ca, carts[i].ca);
      Point scaled = scale(unitVector(unscaled), Converter.BOND_LEN);
      Point shift = pointSubtract(scaled, unscaled);
      for (int j = i+1; j < carts.length; j++) {
        carts[j].ca = pointAdd(carts[j].ca, shift);
      }
    }
  }

  public static double piecewisePartial1(double k, double floor, double dist) {
    return 2 * k - 2 * k * floor / dist;
  }

  public static double piecewisePartial3(double k, double ceil, double dist) {
    return 2 * k - 2 * k * ceil / dist;
  }

  public static double piecewisePartial4(double k, double dist) {
    return 2 * k / dist;
  }

  public static double[] getGradient(int i, int j) {
    Point p = pointSubtract(carts[j].ca, carts[i].ca);
    double floor = Scoring.contactTable[i][j][0];
    double ceil = Scoring.contactTable[i][j][1];
    double dist = magnitude(p);
    if (dist < floor) {
      double delX = piecewisePartial1(p.x, floor, dist);
      double delY = piecewisePartial1(p.y, floor, dist);
      double delZ = piecewisePartial1(p.z, floor, dist);
      return new double[]{delX, delY, delZ};
    } else if (dist >= floor && dist <= ceil) {
      return new double[]{0.0, 0.0, 0.0};
    } else if (dist > ceil + 0.5) {
      double delX = piecewisePartial3(p.x, ceil, dist);
      double delY = piecewisePartial3(p.y, ceil, dist);
      double delZ = piecewisePartial3(p.z, ceil, dist);
      return new double[]{delX, delY, delZ};
    } else {
      double delX = piecewisePartial4(p.x, dist);
      double delY = piecewisePartial4(p.y, dist);
      double delZ = piecewisePartial4(p.z, dist);
      return new double[]{delX, delY, delZ};
    }
  }

  public static void applyGradient(int i, double[] gradient) {
    double x = carts[i].ca.x - stepSize * gradient[0];
    double y = carts[i].ca.y - stepSize * gradient[1];
    double z = carts[i].ca.z - stepSize * gradient[2];
    carts[i].ca = new Point(x, y, z);
  }

  public static void setIterations(double i) { iterations = (int) i; }
  public static void setStepSize(double alpha) { stepSize = alpha; }
}