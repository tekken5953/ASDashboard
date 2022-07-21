package com.example.dashboard.bluetooth;

/**     AQI 값 범위 별 구간
 *
 *            0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - 500(매우나쁨)
 * PM 2.5       0 - 15         15 - 35            35 - 75           70 - 500
 * CO2         0 - 1000      1000 - 1500        1500 - 2000       2000 - 5000
 * TVOC        0 - 0.25      0.25 - 0.5          0.5 - 0.6         0.6 - 4.0
 * CO          0 - 4.5        4.5 - 9.0          9.0 - 10.8        10.8 - 50
 */

/**       PM 2.5 AQI 계산
 *
 *            0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - (매우나쁨)
 * Y            c * 0.05       c * 0.1            c * 0.3         c * 0.5
 * Z           t * 0.025       t * 0.05           t * 0.15        t * 0.25
 */

/**       wFactor 계산
 *
 *  humid/temp     - 10       10 - 19       19 - 20      20       20 - 24      24 - 50      50 -
 * 0 - 20            2          1.4           1.25       1.2        1.25         1.4         2
 * 20 - 40           1          0.9           0.7        0.6        0.7          0.9         1
 * 40 - 50          0.4         0.35          0.3        0.25       0.3          0.35       0.4
 * 50               0.35        0.3          0.25        0.2        0.25         0.3        0.35
 * 50 - 60          0.4         0.35          0.3        0.25       0.3          0.35       0.4
 * 60 - 80           1          0.9           0.7        0.6        0.7          0.9         1
 * 80 - 100          2          1.4          1.25        1.2        1.25         1.4         2
 */

public class VirusFormulaClass {

    /* Virus Value = float((X + Y + Z) * wFactor / 1750 * 100)
       Virus Index = int(Virus Value) / 10 + 1) */

    // CO2의 AQI 값 범위 계산
    private double GetY(float co2IndexFloat) {
        //범위 내의 값
        if (co2IndexFloat >= 0 && co2IndexFloat <= 5000) {
            // 좋음
            if (co2IndexFloat <= 1000) {
                return co2IndexFloat * 0.05;
            }
            // 보통
            else if (co2IndexFloat <= 1500) {
                return co2IndexFloat * 0.1;
            }
            // 나쁨
            else if (co2IndexFloat <= 2000) {
                return co2IndexFloat * 0.3;
            }
            // 매우나쁨
            else {
                return co2IndexFloat * 0.5;
            }
        } else {
            // 범위 밖
            return -1;
        }
    }

    // TVOC의 AQI 값 범위 계산
    private double GetZ(float tvocIndexFloat) {
        //범위 내의 값
        if (tvocIndexFloat >= 0 && tvocIndexFloat <= 4.0) {
            // 좋음
            if (tvocIndexFloat <= 0.25) {
                return tvocIndexFloat * 0.025;
            }
            // 보통
            else if (tvocIndexFloat <= 0.5) {
                return tvocIndexFloat * 0.05;
            }
            // 나쁨
            else if (tvocIndexFloat <= 0.6) {
                return tvocIndexFloat * 0.15;
            }
            // 매우나쁨
            else {
                return tvocIndexFloat * 0.25;
            }
        } else {
            // 범위 밖
            return -1;
        }
    }

    // 온습도 바이러스 지수 영향력 계산
    private double GetWFactor(float temp, float humid) {
        // 온도 영향력 매우높음
        if (temp < 10 || temp > 50) {
            // 습도 영향력 매우높음
            if ((humid >= 0 && humid < 20) || (humid > 80 && humid <= 100)) {
                return 2;
            }
            // 습도 영향력 높음
            else if ((humid >= 20 && humid < 40) || (humid > 60 && humid <= 80)) {
                return 1;
            }
            // 습도 영향력 보통
            else if ((humid >= 40 && humid < 50) || (humid > 50 && humid <= 60)) {
                return 0.4;
            }
            // 습도 영향력 낮음
            else if (humid == 50) {
                return 0.35;
            }
        }
        //온도 영향력 높음
        else if ((temp >= 10 && temp < 19) || (temp > 24)) {
            // 습도 영향력 매우높음
            if ((humid >= 0 && humid < 20) || (humid > 80 && humid <= 100)) {
                return 1.4;
            }
            // 습도 영향력 높음
            else if ((humid >= 20 && humid < 40) || (humid > 60 && humid <= 80)) {
                return 0.9;
            }
            // 습도 영향력 보통
            else if ((humid >= 40 && humid < 50) || (humid > 50 && humid <= 60)) {
                return 0.35;
            }
            // 습도 영향력 낮음
            else if (humid == 50) {
                return 0.3;
            }
        }
        //온도 영향력 보통
        else if ((temp >= 19 && temp < 20) || (temp > 20)) {
            // 습도 영향력 매우높음
            if ((humid >= 0 && humid < 20) || (humid > 80 && humid <= 100)) {
                return 1.25;
            }
            // 습도 영향력 높음
            else if ((humid >= 20 && humid < 40) || (humid > 60 && humid <= 80)) {
                return 0.7;
            }
            // 습도 영향력 보통
            else if ((humid >= 40 && humid < 50) || (humid > 50 && humid <= 60)) {
                return 0.3;
            }
            // 습도 영향력 낮음
            else if (humid == 50) {
                return 0.25;
            }
        }
        //온도 영향력 낮음
        else {
            // 습도 영향력 매우높음
            if ((humid >= 0 && humid < 20) || (humid > 80 && humid <= 100)) {
                return 1.2;
            }
            // 습도 영향력 높음
            else if ((humid >= 20 && humid < 40) || (humid > 60 && humid <= 80)) {
                return 0.6;
            }
            // 습도 영향력 보통
            else if ((humid >= 40 && humid < 50) || (humid > 50 && humid <= 60)) {
                return 0.25;
            }
            // 습도 영향력 낮음
            else if (humid == 50) {
                return 0.2;
            }
        }
        return -1;
    }

    // Virus Value 계산
    public float GetVirusValue(float pmAQI, float temp, float humid, float co2, float tvoc){
        double y = GetY(co2);
        double z = GetZ(tvoc);
        double wFactor = GetWFactor(temp, humid);
        return (float) ((pmAQI + y + z) * wFactor / 1750 * 100);
    }

    //Virus Index 계산
    public String GetVirusIndex(float pmAQI, float temp, float humid, float co2, float tvoc) {

        int virusIndex = (int) (GetVirusValue(pmAQI, temp, humid, co2, tvoc) / 10 + 1);

        if (virusIndex >= 1 && virusIndex <= 3) {
            return "0";
        }
        else if (virusIndex >= 4 && virusIndex <= 6) {
            return "1";
        }
        else if (virusIndex >= 7 && virusIndex <= 8) {
            return "2";
        }
        else if (virusIndex >= 9 && virusIndex <= 10) {
            return "3";
        }
        else {
            return "4";
        }
    }

    //CO AQI Index 변형
    private double TransformCoAqiToIndex(float co) {
        // 범위 내의 값
        if (co >= 0 && co <= 50) {
            // 좋음
            if (co <= 4.5) {
                return (co / 4.5) * 50;
            }
            // 보통
            else if (co <= 9.0) {
                return ((co - 4.5) / 4.5 ) * 50 + 50;
            }
            // 나쁨
            else if (co <= 10.8) {
                return ((co - 9.0) / 1.8 ) * 150 + 100;
            }
            // 매우나쁨
            else {
                return ((co - 10.8) / 39.2 ) * 250 + 250;
            }
        }
        // 범위 밖
        return -1;
    }

    // CQI 선정 및 계산
    public int GetCQIValue(int pm, float co) {
        int coIndex = (int) TransformCoAqiToIndex(co);
        // PM은 100보다 작고 CO가 100보다 큰 경우
        if (pm < 100 && coIndex >= 100) {
            return coIndex;
        }
        // CO가 100보다 작고 PM은 100보다 큰 경우
        else if (pm >= 100 && coIndex < 100) {
            return pm;
        }
        // 둘다 100보다 작은 경우
        else if (pm < 100 && coIndex < 100) {
            if (pm >= coIndex) {
                return pm;
            } else {
                return coIndex;
            }
        }
        // 둘다 100보다 큰 경우
        else {
            if (pm >= coIndex) {
                return pm + 25;
            } else {
                return coIndex + 25;
            }
        }
    }

    public String GetCQIGrade(int pm, float co) {
        int cqi = GetCQIValue(pm, co);
        if (cqi >= 0 && cqi <= 50) {
            return "0";
        }
        else if (cqi > 50 && cqi <= 100) {
            return "1";
        }
        else if (cqi > 100 && cqi <= 250) {
            return "2";
        }
        else if (cqi > 250 && cqi <= 500) {
            return "3";
        } else {
            return "4";
        }
    }
}