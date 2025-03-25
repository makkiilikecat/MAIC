package com.makkii.maic;

public class Matrix {
    private final int rows;             // 行数
    private final int cols;             // 列数
    private final double[][] data;   // データ

    // コンストラクタ
    public Matrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    // 配列からMatrixを生成するコンストラクタ
    public Matrix(double[][] data) {
        this.rows = data.length;
        this.cols = data[0].length;
        this.data = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, this.data[i], 0, cols);
        }
    }

    // 行列の要素を取得
    public double get(int i, int j) {
        return data[i][j];
    }

    // 行列の要素を設定
    public void set(int i, int j, double value) {
        data[i][j] = value;
    }

    // 行数を取得
    public int getRowDimension() {
        return rows;
    }

    // 列数を取得
    public int getColumnDimension() {
        return cols;
    }

    // 行列の加算 (this + B)
    public Matrix plus(Matrix B) {
        Matrix A = this;
        if (B.rows != A.rows || B.cols != A.cols) {
            throw new IllegalArgumentException("Matrix dimensions must agree.");
        }
        Matrix C = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                C.data[i][j] = A.data[i][j] + B.data[i][j];
            }
        }
        return C;
    }

    // 行列の減算 (this - B)
    public Matrix minus(Matrix B) {
        Matrix A = this;
        if (B.rows != A.rows || B.cols != A.cols) {
            throw new IllegalArgumentException("Matrix dimensions must agree.");
        }
        Matrix C = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                C.data[i][j] = A.data[i][j] - B.data[i][j];
            }
        }
        return C;
    }

    // 行列の乗算 (this * B)
    public Matrix times(Matrix B) {
        Matrix A = this;
        if (A.cols != B.rows) {
            throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        }
        Matrix C = new Matrix(A.rows, B.cols);
        for (int i = 0; i < C.rows; i++) {
            for (int j = 0; j < C.cols; j++) {
                for (int k = 0; k < A.cols; k++) {
                    C.data[i][j] += A.data[i][k] * B.data[k][j];
                }
            }
        }
        return C;
    }

    // ベクトルとの積 (this * x)
    public static void times(double[] x, Matrix A, double[] y) {
        if (A.data.length != y.length) throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        if (A.data[0].length != x.length) throw new IllegalArgumentException("Matrix inner dimensions must agree.");
        for (int i = 0; i < A.rows; i++) {
            for (int j = 0; j < A.cols; j++) {
                y[i] += A.data[i][j] * x[j];
            }
        }
    }

    // 転置行列
    public Matrix transpose() {
        Matrix A = this;
        Matrix C = new Matrix(A.cols, A.rows);
        for (int i = 0; i < A.rows; i++) {
            for (int j = 0; j < A.cols; j++) {
                C.data[j][i] = A.data[i][j];
            }
        }
        return C;
    }

    // 行列の表示
    public void print() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.printf("%9.6f ", data[i][j]);
            }
            System.out.println();
        }
    }

    // ゼロ行列
    public static Matrix getZeroMatrix(int rows, int cols) {
        return new Matrix(rows, cols);
    }

    //単位行列
    public static Matrix getIdentityMatrix(int size) {
        Matrix m = new Matrix(size, size);
        for (int i = 0; i < size; i++) {
            m.set(i, i, 1);
        }
        return m;
    }

    //乱数行列
    public static Matrix getRandomMatrix(int rows, int cols, double d) {
        Matrix m = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.set(i, j, (Math.random() - 0.5) * d);
            }
        }
        return m;
    }
}