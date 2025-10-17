package Assignments.Assignment4;


public class WalshCode {
    private int[][] matrix;
    private int size;

    public WalshCode(int totalStations) {
        int s = 1;
        while (s < totalStations) s <<= 1;
        this.size = s;
        matrix = new int[s][s];
        matrix[0][0] = 1;
        int current = 1;
        while (current < s) {
            for (int i = 0; i < current; i++) {
                for (int j = 0; j < current; j++) {
                    int v = matrix[i][j];
                    matrix[i][j + current] = v;
                    matrix[i + current][j] = v;
                    matrix[i + current][j + current] = -v;
                }
            }
            current <<= 1;
        }
    }

    public int[] getCode(int stationIndex) {
        if (stationIndex < 0 || stationIndex >= size)
            throw new IllegalArgumentException("stationIndex out of range");
        return matrix[stationIndex];
    }
}
