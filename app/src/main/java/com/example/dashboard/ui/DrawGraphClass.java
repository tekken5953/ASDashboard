package com.example.dashboard.ui;

import android.app.Activity;
import android.graphics.Color;

import androidx.core.content.res.ResourcesCompat;

import com.example.dashboard.R;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.Timer;

public class DrawGraphClass {
    // https://medium.com/hongbeomi-dev/mpandroidchart-%EB%9D%BC%EC%9D%B4%EB%B8%8C%EB%9F%AC%EB%A6%AC%EB%A5%BC-%ED%99%9C%EC%9A%A9%ED%95%9C-chart-%EC%82%AC%EC%9A%A9%ED%95%98%EA%B8%B0-kotlin-93c18ae7568e

    LineData lineData = new LineData();
    Legend legend = new Legend();
    LineDataSet lineDataSet;
    Timer  chart_scheduler;

    void setChart(int setYMax, com.github.mikephil.charting.charts.LineChart lineChart, Activity activity) {
        // X축
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(true); // 라벨 표시 여부

        xAxis.setTextColor(Color.WHITE);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
        xAxis.setDrawAxisLine(false); // AxisLine 표시
        xAxis.setDrawGridLines(false); // GridLine 표시
        xAxis.setGranularityEnabled(false); // x축 간격을 제한하는 세분화 기능
        xAxis.setGranularity(1);
        lineChart.setAutoScaleMinMaxEnabled(true); // Max = Count
        xAxis.setLabelCount(6); // 라벨 갯수

        lineChart.moveViewToX(lineData.getEntryCount()); // 계속 X축을 데이터의 오른쪽 끝으로 옮기기
        lineChart.setVisibleXRangeMaximum(7); // X축 최대 표현 개수
        lineChart.setPinchZoom(false); // 확대 설정
        lineChart.setDoubleTapToZoomEnabled(false); // 더블탭 설정
        lineChart.getDescription().setEnabled(false); // 차트 값 설명 유효화
        lineChart.setBackgroundColor(Color.TRANSPARENT); // 차트 배경색 설정
        lineChart.setExtraOffsets(5f, 5f, 5f, 5f); // 차트 Padding 설정
        lineChart.setNoDataText(activity.getString(R.string.no_data_text));

        lineChart.getAxisRight().setEnabled(false); // 라인차트 오른쪽 데이터 비활성화
        // Y축
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMaximum(setYMax); // Y축 값 최대값 설정
        yAxis.setAxisMinimum((0)); // Y축 값 최솟값 설정

        yAxis.setTextColor(Color.parseColor("#FFFFFF")); // y축 글자 색상
        yAxis.setValueFormatter(new YAxisValueFormat()); // y축 데이터 포맷
        yAxis.setGranularityEnabled(false); // y축 간격을 제한하는 세분화 기능
        yAxis.setDrawLabels(true); // Y축 라벨 위치
        yAxis.setDrawGridLines(false); // GridLine 표시
        yAxis.setDrawAxisLine(false); // AxisLine 표시

        legend.setTextColor(Color.WHITE);
        legend.setEnabled(false); // 범례 비활성화

        lineChart.setData(lineData); // 라인차트 데이터 설정

    }

    // 차트에 쓰일 목록 UI Thread 에서 가져오기
    public void feedMultiple(int SetYMax, float yData, com.github.mikephil.charting.charts.LineChart lineChart, Activity activity) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                setChart(SetYMax,lineChart,activity);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addEntry(yData,lineChart,activity);
                    }
                });
            }
        });
        thread.start();
    }

    // 엔트리 추가하기
    void addEntry(float yData,com.github.mikephil.charting.charts.LineChart lineChart, Activity activity) {
        lineData = lineChart.getData();
        // 라인 차트
        if (lineData != null) {
            createSet(activity);
            lineData.addDataSet(lineDataSet);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lineData.addEntry(new Entry(lineData.getEntryCount(), yData), 0); // 데이터 엔트리 추가
                    lineData.notifyDataChanged(); // 데이터 변경 알림
                    lineChart.notifyDataSetChanged(); // 라인차트 변경 알림
                }
            });
            
        }
    }

    void createSet(Activity activity) {
        lineDataSet = new LineDataSet(null, null); // 범례, yVals 설정
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT); // Y값 데이터를 왼쪽으로
        lineDataSet.setFillColor(Color.parseColor("#147AD6")); // 차트 채우기 색상
        lineDataSet.setDrawFilled(true); // 차트 채우기 설정
        lineDataSet.setHighlightEnabled(false); // 하이라이트 설정
        lineDataSet.setLineWidth(2F); // 그래프 선 굵기
        lineDataSet.setValueTextColor(Color.TRANSPARENT);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
        lineDataSet.setDrawCircleHole(false); // 원 안에 작은 원 표시
        lineDataSet.setDrawCircles(false); // 원 표시
        lineDataSet.setColor(ResourcesCompat.getColor(activity.getResources(), R.color.lineChartLine, null)); // 색상 지정
    }

    //Y축 엔트리 포멧
    private class YAxisValueFormat extends IndexAxisValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            String newValue = value + "";
            return newValue.substring(0, newValue.length() - 2);
        }
    }

    public void reDrawChart(com.github.mikephil.charting.charts.LineChart lineChart, Activity activity) {
        chart_scheduler.cancel();
        lineData.clearValues();
        lineData.notifyDataChanged();
        lineChart.clear();
        lineChart.notifyDataSetChanged();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.removeAllViews();
            }
        });
    }

    public void drawFirstEntry(int setYMax, float yData, com.github.mikephil.charting.charts.LineChart lineChart, Activity activity) {
        if (yData != 0) {
            feedMultiple(setYMax, yData, lineChart, activity);
        }
    }
}