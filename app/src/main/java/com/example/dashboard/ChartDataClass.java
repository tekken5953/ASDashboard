//package com.example.dashboard;
//
//import android.annotation.SuppressLint;
//import android.graphics.Color;
//import android.util.Log;
//
//import com.github.mikephil.charting.components.Legend;
//import com.github.mikephil.charting.components.XAxis;
//import com.github.mikephil.charting.components.YAxis;
//import com.github.mikephil.charting.data.Entry;
//import com.github.mikephil.charting.data.LineData;
//import com.github.mikephil.charting.data.LineDataSet;
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
//import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//
//public class ChartDataClass {
//
//    //Line Chart Values
//    ArrayList<Entry> chartData = new ArrayList<>();
//    ArrayList<ILineDataSet> lineDataSet = new ArrayList<>();
//    LineData lineData;
//    Legend legend = new Legend();
//
//    //LineChart Draw
//    // 차트 데이터 초기화
//    void initChartData() {
//        // 차트 그리는 엔트리 부분
//        addLastData();
//
//        if (lineDataSet.isEmpty()) {
//            LineDataSet set = new LineDataSet(chartData, null);
//            lineDataSet.add(set);
//            lineData = new LineData(lineDataSet);
//
//            set.setFillColor(Color.parseColor("#147AD6")); // 차트 색상
//            set.setDrawFilled(true);
//            set.setHighlightEnabled(false);
//            set.setLineWidth(2F); // 그래프 선 굵기
//            set.setDrawValues(false); // 차트에 값 표시
//            set.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 선 그리는 방식
//            set.setDrawCircleHole(false); // 원 안에 작은 원 표시
//            set.setDrawCircles(false); // 원 표시
//            set.setColor(Color.parseColor("#147AD6"));
//        }
//    }
//
//    void addLastData() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.e("Timertask", "index : " + chartIndex + ", x : " + (900 * chartXDataFloat / 24) + ", y : " + aqi_short);
//
//                if (chartIndex > 6) {
//                    chartData.add(chartIndex, new Entry(900 * chartXDataFloat / 24, aqi_short));
//                } else {
//                    chartData.add(chartIndex, new Entry(900 * chartXDataFloat / 24, aqi_short));
//                    chartIndex++;
//                }
//                binding.virusLineChart.notifyDataSetChanged();
//                binding.virusLineChart.postInvalidate();
//            }
//        });
//    }
//
//    void removeFirstData() {
//        chartData.remove(0);
//    }
//
//    // 차트 처리
//    private void initChart(int setYMax) {
//        binding.virusLineChart.setAutoScaleMinMaxEnabled(false);
//        binding.virusLineChart.setDrawGridBackground(false);
//        binding.virusLineChart.setBackgroundColor(Color.TRANSPARENT);
//        binding.virusLineChart.setDrawBorders(false);
//        binding.virusLineChart.setDragEnabled(false);
//        binding.virusLineChart.setTouchEnabled(false);
//        binding.virusLineChart.setScaleEnabled(false);
//
//        legend.setEnabled(false);
//
//        // X축
//        XAxis xAxis = binding.virusLineChart.getXAxis();
//        xAxis.setDrawLabels(true); // 라벨 표시 여부
//        xAxis.setAxisMaximum(chartXMax);
//        xAxis.setAxisMinimum(chartXMin);
//        xAxis.setLabelCount(7, true); // 라벨 갯수
//
//        xAxis.setTextColor(Color.WHITE);
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // X축 라벨 위치
//        xAxis.setDrawAxisLine(false); // AxisLine 표시
//        xAxis.setDrawGridLines(false); // GridLine 표시
//        xAxis.setValueFormatter(new TimeAxisValueFormat());
//
//        // Y축
//        YAxis yAxis = binding.virusLineChart.getAxisLeft();
//        yAxis.setAxisMaximum(setYMax);
//        yAxis.setAxisMinimum((0));
//
//        yAxis.setTextColor(Color.parseColor("#FFFFFF"));
//        yAxis.setValueFormatter(new YAxisValueFormat());
//        yAxis.setGranularityEnabled(false);
//        yAxis.setDrawLabels(true); // Y축 라벨 위치
//        yAxis.setDrawGridLines(false); // GridLine 표시
//        yAxis.setDrawAxisLine(false); // AxisLine 표시
//
//        // 오른쪽 Y축 값
//        YAxis yRAxisVal = binding.virusLineChart.getAxisRight();
//        yRAxisVal.setDrawLabels(false);
//        yRAxisVal.setDrawAxisLine(false);
//        yRAxisVal.setDrawGridLines(false);
//
//        binding.virusLineChart.getDescription().setEnabled(false); // 설명
//        binding.virusLineChart.setData(lineData); // 데이터 설정
//        initChartData(); // 차트 초기화
//        binding.virusLineChart.invalidate(); // 다시 그리기
//    }
//
//    //Y축 엔트리 포멧
//    private class YAxisValueFormat extends IndexAxisValueFormatter {
//        @Override
//        public String getFormattedValue(float value) {
//            String newValue = value + "";
//            return newValue.substring(0, newValue.length() - 2);
//        }
//    }
//
//    // X축 엔트리 포멧
//    private class TimeAxisValueFormat extends IndexAxisValueFormatter {
//        @Override
//        public String getFormattedValue(float value) {
//            long valueToMinutes = TimeUnit.MINUTES.toMillis((long) value);
//            Date timeMinutes = new Date(valueToMinutes);
//            @SuppressLint("SimpleDateFormat") SimpleDateFormat formatMinutes = new SimpleDateFormat("HH:mm");
//            return formatMinutes.format(timeMinutes);
//        }
//    }
//
//    private void removeChartView() {
//        chartData.clear();
//    }
//}
