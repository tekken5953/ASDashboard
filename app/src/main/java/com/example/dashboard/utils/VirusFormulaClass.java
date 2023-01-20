package com.example.dashboard.utils;
/**
 * AQI 값 범위 별 구간
 * <p>
 * 0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - 500(매우나쁨)
 * PM 2.5       0 - 15         15 - 35            35 - 75           70 - 500
 * CO2         0 - 1000      1000 - 1500        1500 - 2000       2000 - 5000
 * TVOC        0 - 0.25      0.25 - 0.5          0.5 - 0.6         0.6 - 4.0
 * CO          0 - 4.5        4.5 - 9.0          9.0 - 10.8        10.8 - 50
 * PM 2.5 AQI 계산
 * <p>
 * 0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - (매우나쁨)
 * Y            c * 0.05       c * 0.1            c * 0.3         c * 0.5
 * Z           t * 0.025       t * 0.05           t * 0.15        t * 0.25
 * PM 2.5 AQI 계산
 * <p>
 * 0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - (매우나쁨)
 * Y            c * 0.05       c * 0.1            c * 0.3         c * 0.5
 * Z           t * 0.025       t * 0.05           t * 0.15        t * 0.25
 * PM 2.5 AQI 계산
 * <p>
 * 0 - 50(좋음)    50 - 100 (보통)    100 - 250(나쁨)   250 - (매우나쁨)
 * Y            c * 0.05       c * 0.1            c * 0.3         c * 0.5
 * Z           t * 0.025       t * 0.05           t * 0.15        t * 0.25
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

    private float GetCo2AQI(float co2) {
        if (co2 >= 0 && co2 <= 1000) {
            co2 = 0 + (co2 - 0) * (50 - 0) / (1000 - 0);
            return co2;
        } else if (co2 > 1000 && co2 <= 1500) {
            co2 = 50 + (co2 - 1000) * (100 - 50) / (1500 - 1000);
            return co2;
        } else if (co2 > 1500 && co2 <= 2000) {
            co2 = 100 + (co2 - 1500) * (250 - 100) / (2000 - 1500);
            return co2;
        } else if (co2 > 2000 && co2 <= 5000) {
            co2 = 250 + (co2 - 2000) * (500 - 250) / (5000 - 2000);
            return co2;
        }
        return -1;
    }

    private double GetTvocAQI(double tvoc) {
        if (tvoc >= 0 && tvoc <= 0.25) {
            tvoc = 0 + (tvoc - 0) * (50 - 0) / (0.25 - 0);
            return tvoc;
        } else if (tvoc > 0.25 && tvoc <= 0.5) {
            tvoc = 50 + (tvoc - 0.25) * (100 - 50) / (0.5 - 0.25);
            return tvoc;
        } else if (tvoc > 0.5 && tvoc <= 0.6) {
            tvoc = 100 + (tvoc - 0.5) * (250 - 100) / (0.6 - 0.5);
            return tvoc;
        } else if (tvoc > 0.6 && tvoc <= 4.0) {
            tvoc = 250 + (tvoc - 0.6) * (500 - 250) / (4.0 - 0.6);
            return tvoc;
        }
        return -1;
    }


    // CO2의 AQI 값 범위 계산
    private double GetY(float co2Value, float pmAQI) {
        float co2AQI = GetCo2AQI(co2Value);
        //범위 내의 값
        if (co2AQI >= 0 && co2AQI <= 500 && pmAQI >= 0) {
            // 좋음
            if (pmAQI <= 50) {
                return co2AQI * 0.05;
            }
            // 보통
            else if (pmAQI <= 100) {
                return co2AQI * 0.1;
            }
            // 나쁨
            else if (pmAQI <= 250) {
                return co2AQI * 0.3;
            }
            // 매우나쁨
            else {
                return co2AQI * 0.5;
            }
        }
        //범위 밖
        else {
            return -1;
        }
    }

    // TVOC 의 AQI 값 범위 계산
    private double GetZ(float tvocVlaue, float pmAQI) {
        double tvocAQI = GetTvocAQI(tvocVlaue);
        //범위 내의 값
        if (tvocAQI >= 0 && tvocAQI <= 500 && pmAQI >= 0) {
            // 좋음
            if (pmAQI <= 50) {
                return tvocAQI * 0.25;
            }
            // 보통
            else if (pmAQI <= 100) {
                return tvocAQI * 0.05;
            }
            // 나쁨
            else if (pmAQI <= 250) {
                return tvocAQI * 0.15;
            }
            // 매우나쁨
            else {
                return tvocAQI * 0.25;
            }
        }
        //범위 밖
        else {
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
    public int GetVirusValue(float pmAQI, float temp, float humid, float co2, float tvoc) {
        double y = GetY(co2, pmAQI);
        double z = GetZ(tvoc, pmAQI);
        double wFactor = GetWFactor(temp, humid);
        float k = (float) ((pmAQI + y + z) * wFactor / 1750 * 100);
        return Math.round(k / 10 + 1);
    }

    //Virus Index 계산
    public String GetVirusGrade(float pmAQI, float temp, float humid, float co2, float tvoc) {

        long virusIndex = GetVirusValue(pmAQI, temp, humid, co2, tvoc);

        if (virusIndex >= 0 && virusIndex <= 3) {
            return "0";
        } else if (virusIndex >= 4 && virusIndex <= 6) {
            return "1";
        } else if (virusIndex >= 7 && virusIndex <= 8) {
            return "2";
        } else if (virusIndex >= 9 && virusIndex <= 10) {
            return "3";
        } else {
            return "4";
        }
    }

    //CO AQI Index 변형
    private double TransformCoAqiToIndex(double co) {
        if (co <= 50 && co >= 0) {
            if (co <= 4.5) {
                co = 0 + (co - 0) * (50 - 0) / (4.5 - 0);
                return co;
            } else if (co <= 9.0) {
                co = 50 + (co - 4.5) * (100 - 50) / (9.0 - 4.5);
                return co;
            } else if (co <= 10.8) {
                co = 100 + (co - 9.0) * (250 - 100) / (10.8 - 9.0);
                return co;
            } else {
                co = 250 + (co - 10.8) * (500 - 250) / (50 - 10.8);
                return co;
            }
        }
        return -1;
    }

    public float TransformPmIndexToAQI(float pm) {
        if (pm <= 500 && pm >= 0) {
            if (pm <= 15) {
                pm = 0 + (pm - 0) * (50 - 0) / (15 - 0);
                return pm;
            } else if (pm <= 35) {
                pm = 50 + (pm - 15) * (100 - 50) / (35 - 15);
                return pm;
            } else if (pm <= 75) {
                pm = 100 + (pm - 35) * (250 - 100) / (75 - 35);
                return pm;
            } else {
                pm = 250 + (pm - 75) * (500 - 250) / (500 - 75);
                return pm;
            }
        }
        return -1;
    }

    // CQI 선정 및 계산
    public int GetCQIValue(float pm, float co) {
        float pmAQI = TransformPmIndexToAQI(pm);
        double coAQI = TransformCoAqiToIndex(co);
        // PM은 100보다 작고 CO가 100보다 큰 경우
        if (pmAQI < 100 && coAQI >= 100) {
            return (int) Math.round(coAQI);
        }
        // CO가 100보다 작고 PM은 100보다 큰 경우
        else if (pmAQI >= 100 && coAQI < 100) {
            return Math.round(pmAQI);
        }
        // 둘다 100보다 작은 경우
        else if (pmAQI < 100) {
            if (pmAQI >= coAQI) {
                return Math.round(pmAQI);
            } else {
                return (int) Math.round(coAQI);
            }
        }
        // 둘다 100보다 큰 경우
        else {
            if (pmAQI >= coAQI) {
                return Math.round(pmAQI + 25);
            } else {
                return (int) Math.round(coAQI + 25);
            }
        }
    }

    public String GetCQIGrade(float pm, float co) {
        int cqi = GetCQIValue(pm, co);
        if (cqi >= 0 && cqi <= 50) {
            return "0";
        } else if (cqi > 50 && cqi <= 100) {
            return "1";
        } else if (cqi > 100 && cqi <= 250) {
            return "2";
        } else if (cqi > 250 && cqi <= 500) {
            return "3";
        } else {
            return "4";
        }
    }
}