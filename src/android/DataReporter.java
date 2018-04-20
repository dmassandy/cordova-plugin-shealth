/**
 * Copyright (C) 2014 Samsung Electronics Co., Ltd. All rights reserved.
 *
 * Mobile Communication Division,
 * Digital Media & Communications Business, Samsung Electronics Co., Ltd.
 *
 * This software and its documentation are confidential and proprietary
 * information of Samsung Electronics Co., Ltd.  No part of the software and
 * documents may be copied, reproduced, transmitted, translated, or reduced to
 * any electronic medium or machine-readable form without the prior written
 * consent of Samsung Electronics.
 *
 * Samsung Electronics makes no representations with respect to the contents,
 * and assumes no responsibility for any errors that might appear in the
 * software and documents. This publication and the contents hereof are subject
 * to change without notice.
 */

package com.samsung.android.simplehealth;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import android.app.AlertDialog;
import android.database.Cursor;
import android.util.Log;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.apache.cordova.*;

import android.app.Activity;

import java.util.Calendar;

public class DataReporter {
    private final HealthDataStore mStore;

    Activity activity;
    CallbackContext callbackContext;
    CallbackContext observerCallbackContext;
    boolean isObserverAdded = false;

    String APP_TAG = "CordovaSHealthPlugin";

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {

         // Checks notification for changed health data
         @Override
         public void onChange(String dataTypeName) {
            Log.d(APP_TAG, "Health data is changed: " + dataTypeName);
            if (observerCallbackContext != null) {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, dataTypeName);
                pluginResult.setKeepCallback(true);
                observerCallbackContext.sendPluginResult(pluginResult);
            }
         }
     };

    /** Default Constructor.
     *
     * @param store             The connected {@link HealthDataStore}
     * @param pActivity         Activity of the cordova application
     * @param pCallbackContext  Object holding callback functions
     */
    public DataReporter(HealthDataStore store, Activity pActivity, CallbackContext pCallbackContext) {
        mStore = store;
        activity = pActivity;
        this.callbackContext = pCallbackContext;
    }

    /** Set callback context
     */
     public void setCallbackContext(CallbackContext pCallbackContext) {
         this.callbackContext = pCallbackContext;
     }

    /** Start observer to listen any data changed
     * @param healthDataType    valid Health Data Type
     * @param pCallbackContext  callback for observer
     */
     public void startObserver(String[] healthDataTypes, CallbackContext pCallbackContext) {
         if (isObserverAdded) {
             removeObserver();
         }
         this.observerCallbackContext = pCallbackContext;
         for (int i = 0; i < healthDataTypes.length; ++i) {
             HealthDataObserver.addObserver(mStore,healthDataTypes[i],mObserver);
         }
         
         isObserverAdded = true;
     }

    /** Remove registered observer
     */
     public void removeObserver() {
         if (isObserverAdded) {
            HealthDataObserver.removeObserver(mStore,mObserver);
            observerCallbackContext = null;
            isObserverAdded = false;
         }
     }

    /** Initiates the database query
     *
     * @param pStartTime    Earliest time of measurement
     * @param pEndTime      Latest time of measurement
     */
    public void start(String hcHDT, long pStartTime, long pEndTime) {
        Log.d(APP_TAG,"Time: " + pStartTime + " - " + pEndTime);
        if (hcHDT.equals(HealthConstants.StepCount.HEALTH_DATA_TYPE)) {
            startReadStepCount(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.Exercise.HEALTH_DATA_TYPE)) {
            startReadExercise(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.Sleep.HEALTH_DATA_TYPE)) {
            startReadSleep(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.SleepStage.HEALTH_DATA_TYPE)) {
            startReadSleepStage(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.FoodIntake.HEALTH_DATA_TYPE)) {
            startReadFoodIntake(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.WaterIntake.HEALTH_DATA_TYPE)) {
            startReadWaterIntake(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.CaffeineIntake.HEALTH_DATA_TYPE)) {
            startReadCaffeineIntake(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.HeartRate.HEALTH_DATA_TYPE)) {
            startReadHeartRate(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.BodyTemperature.HEALTH_DATA_TYPE)) {
            startReadBodyTemperature(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.BloodPressure.HEALTH_DATA_TYPE)) {
            startReadBloodPressure(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.BloodGlucose.HEALTH_DATA_TYPE)) {
            startReadBloodGlucose(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.OxygenSaturation.HEALTH_DATA_TYPE)) {
            startReadOxygenSaturation(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.HbA1c.HEALTH_DATA_TYPE)) {
            startReadHbA1c(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE)) {
            startReadAmbientTemperature(pStartTime, pEndTime);
        } else if (hcHDT.equals(HealthConstants.UvExposure.HEALTH_DATA_TYPE)) {
            startReadUvExposure(pStartTime, pEndTime);
        } else if (hcHDT.equals("com.samsung.shealth.step_daily_trend")) {
            startReadStepCountTrend(pStartTime, pEndTime);
        } else {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "{\"TYPE\":\"ERROR\",\"MESSAGE\":\"Health data type not recognized: "+hcHDT+" \"}");
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    }

    public void startReadStepCount(long pStartTime, long pEndTime) {
        // StepCount
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.StepCount.START_TIME,
                HealthConstants.StepCount.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.StepCount.START_TIME,
                        HealthConstants.StepCount.END_TIME,
                        HealthConstants.StepCount.TIME_OFFSET,
                        HealthConstants.StepCount.COUNT,
                        HealthConstants.StepCount.DISTANCE,
                        HealthConstants.StepCount.CALORIE,
                        HealthConstants.StepCount.SPEED,
                        HealthConstants.StepCount.SAMPLE_POSITION_TYPE,
                        HealthConstants.StepCount.DEVICE_UUID,
                        HealthConstants.StepCount.UUID,
                        HealthConstants.StepCount.CREATE_TIME,
                        HealthConstants.StepCount.UPDATE_TIME
                },
                mListenerStepCount
        );
    }

    public void startReadStepCountTrend(long pStartTime, long pEndTime) {

        Filter filter1 = Filter.and(Filter.greaterThanEquals("day_time", pStartTime),
                Filter.lessThan("day_time", pEndTime));
        Filter filter = Filter.and(filter1, Filter.eq("source_type", -2));        

        // StepCount
        readHealthConstantWithFilter(
                filter,
                "com.samsung.shealth.step_daily_trend",
                new String[] {
                        "datauuid",
                        "create_time",
                        "update_time",
                        "deviceuuid",
                        "day_time",
                        "count",
                        "calorie",
                        "distance",
                        "source_type",
                        "speed"
                },
                mListenerStepCountTrend
        );
    }

    public void startReadExercise(long pStartTime, long pEndTime) {
        // Exercise
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.Exercise.START_TIME,
                HealthConstants.Exercise.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.Exercise.START_TIME,
                        HealthConstants.Exercise.END_TIME,
                        HealthConstants.Exercise.TIME_OFFSET,
                        HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.DURATION,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE,
                        HealthConstants.Exercise.DISTANCE,
                        HealthConstants.Exercise.ALTITUDE_GAIN,
                        HealthConstants.Exercise.ALTITUDE_LOSS,
                        HealthConstants.Exercise.COUNT,
                        HealthConstants.Exercise.COUNT_TYPE,
                        HealthConstants.Exercise.MAX_SPEED,
                        HealthConstants.Exercise.MEAN_SPEED,
                        HealthConstants.Exercise.MAX_CALORICBURN_RATE,
                        HealthConstants.Exercise.MEAN_CALORICBURN_RATE,
                        HealthConstants.Exercise.MAX_CADENCE,
                        HealthConstants.Exercise.MEAN_CADENCE,
                        HealthConstants.Exercise.MAX_HEART_RATE,
                        HealthConstants.Exercise.MEAN_HEART_RATE,
                        HealthConstants.Exercise.MIN_HEART_RATE,
                        HealthConstants.Exercise.MAX_ALTITUDE,
                        HealthConstants.Exercise.MIN_ALTITUDE,
                        HealthConstants.Exercise.INCLINE_DISTANCE,
                        HealthConstants.Exercise.DECLINE_DISTANCE,
                        HealthConstants.Exercise.MAX_POWER,
                        HealthConstants.Exercise.MEAN_POWER,
                        HealthConstants.Exercise.MEAN_RPM,
                        HealthConstants.Exercise.LOCATION_DATA
                },
                mListenerExercise
        );
    }

    public void startReadSleep(long pStartTime, long pEndTime) {
        // Sleep
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.Sleep.START_TIME,
                HealthConstants.Sleep.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.Sleep.START_TIME,
                        HealthConstants.Sleep.END_TIME,
                        HealthConstants.Sleep.TIME_OFFSET,
                        HealthConstants.Sleep.DEVICE_UUID,
                        HealthConstants.Sleep.UUID,
                        HealthConstants.Sleep.CREATE_TIME,
                        HealthConstants.Sleep.UPDATE_TIME
                },
                mListenerSleep
        );
    }

    public void startReadSleepStage(long pStartTime, long pEndTime) {
        // SleepStage
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.SleepStage.START_TIME,
                HealthConstants.SleepStage.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.SleepStage.START_TIME,
                        HealthConstants.SleepStage.END_TIME,
                        HealthConstants.SleepStage.TIME_OFFSET,
                        HealthConstants.SleepStage.SLEEP_ID,
                        HealthConstants.SleepStage.STAGE
                },
                mListenerSleepStage
        );
    }

    public void startReadFoodIntake(long pStartTime, long pEndTime) {
        // FoodIntake
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.FoodIntake.START_TIME,
                HealthConstants.FoodIntake.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.FoodIntake.START_TIME,
                        HealthConstants.FoodIntake.TIME_OFFSET,
                        HealthConstants.FoodIntake.CALORIE,
                        HealthConstants.FoodIntake.FOOD_INFO_ID,
                        HealthConstants.FoodIntake.AMOUNT,
                        HealthConstants.FoodIntake.UNIT,
                        HealthConstants.FoodIntake.NAME,
                        HealthConstants.FoodIntake.MEAL_TYPE
                },
                mListenerFoodIntake
        );
    }

    public void startReadWaterIntake(long pStartTime, long pEndTime) {        
        // WaterIntake
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.WaterIntake.START_TIME,
                HealthConstants.WaterIntake.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.WaterIntake.START_TIME,
                        HealthConstants.WaterIntake.TIME_OFFSET,
                        HealthConstants.WaterIntake.AMOUNT,
                        HealthConstants.WaterIntake.UNIT_AMOUNT
                },
                mListenerWaterIntake
        );
    }

    public void startReadCaffeineIntake(long pStartTime, long pEndTime) {   
        // CaffeineIntake     
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.CaffeineIntake.START_TIME,
                HealthConstants.CaffeineIntake.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.CaffeineIntake.START_TIME,
                        HealthConstants.CaffeineIntake.TIME_OFFSET,
                        HealthConstants.CaffeineIntake.AMOUNT,
                        HealthConstants.CaffeineIntake.UNIT_AMOUNT
                },
                mListenerCaffeineIntake
        );
    }

    public void startReadHeartRate(long pStartTime, long pEndTime) { 
        // HeartRate  
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.HeartRate.START_TIME,
                HealthConstants.HeartRate.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.HeartRate.START_TIME,
                        HealthConstants.HeartRate.END_TIME,
                        HealthConstants.HeartRate.TIME_OFFSET,
                        HealthConstants.HeartRate.HEART_RATE,
                        HealthConstants.HeartRate.HEART_BEAT_COUNT
                },
                mListenerHeartRate
        );
    }

    public void startReadBodyTemperature(long pStartTime, long pEndTime) { 
        // BodyTemperature
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.BodyTemperature.START_TIME,
                HealthConstants.BodyTemperature.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.BodyTemperature.START_TIME,
                        HealthConstants.BodyTemperature.TIME_OFFSET,
                        HealthConstants.BodyTemperature.TEMPERATURE
                },
                mListenerBodyTemperature

        );
    }

    public void startReadBloodPressure(long pStartTime, long pEndTime) { 
        // BloodPressure
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.BloodPressure.START_TIME,
                HealthConstants.BloodPressure.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.BloodPressure.START_TIME,
                        HealthConstants.BloodPressure.TIME_OFFSET,
                        HealthConstants.BloodPressure.SYSTOLIC,
                        HealthConstants.BloodPressure.DIASTOLIC,
                        HealthConstants.BloodPressure.MEAN,
                        HealthConstants.BloodPressure.PULSE
                },
                mListenerBloodPressure
        );
    }

    public void startReadBloodGlucose(long pStartTime, long pEndTime) { 
        // BloodGlucose
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.BloodGlucose.START_TIME,
                HealthConstants.BloodGlucose.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.BloodGlucose.START_TIME,
                        HealthConstants.BloodGlucose.TIME_OFFSET,
                        HealthConstants.BloodGlucose.GLUCOSE,
                        HealthConstants.BloodGlucose.MEAL_TIME,
                        HealthConstants.BloodGlucose.MEAL_TYPE,
                        HealthConstants.BloodGlucose.MEASUREMENT_TYPE,
                        HealthConstants.BloodGlucose.SAMPLE_SOURCE_TYPE
                },
                mListenerBloodGlucose
        );
    }

    public void startReadOxygenSaturation(long pStartTime, long pEndTime) { 
        // OxygenSaturation
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.OxygenSaturation.START_TIME,
                HealthConstants.OxygenSaturation.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.OxygenSaturation.START_TIME,
                        HealthConstants.OxygenSaturation.END_TIME,
                        HealthConstants.OxygenSaturation.TIME_OFFSET,
                        HealthConstants.OxygenSaturation.SPO2,
                        HealthConstants.OxygenSaturation.HEART_RATE
                },
                mListenerOxygenSaturation
        );
    }

    public void startReadHbA1c(long pStartTime, long pEndTime) { 
        // HbA1c
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.HbA1c.START_TIME,
                HealthConstants.HbA1c.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.HbA1c.START_TIME,
                        HealthConstants.HbA1c.TIME_OFFSET,
                        HealthConstants.HbA1c.HBA1C
                },
                mListenerHbA1c
        );
    }

    public void startReadAmbientTemperature(long pStartTime, long pEndTime) { 
        // AmbientTemperature
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.AmbientTemperature.START_TIME,
                HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.AmbientTemperature.START_TIME,
                        HealthConstants.AmbientTemperature.TIME_OFFSET,
                        HealthConstants.AmbientTemperature.TEMPERATURE,
                        HealthConstants.AmbientTemperature.HUMIDITY,
                        HealthConstants.AmbientTemperature.LATITUDE,
                        HealthConstants.AmbientTemperature.LONGITUDE,
                        HealthConstants.AmbientTemperature.ALTITUDE,
                        HealthConstants.AmbientTemperature.ACCURACY
                },
                mListenerAmbientTemperature
        );
    }

    public void startReadUvExposure(long pStartTime, long pEndTime) { 
        // UvExposure
        readHealthConstant(
                pStartTime,
                pEndTime,
                HealthConstants.UvExposure.START_TIME,
                HealthConstants.UvExposure.HEALTH_DATA_TYPE,
                new String[] {
                        HealthConstants.UvExposure.START_TIME,
                        HealthConstants.UvExposure.TIME_OFFSET,
                        HealthConstants.UvExposure.UV_INDEX,
                        HealthConstants.UvExposure.LATITUDE,
                        HealthConstants.UvExposure.LONGITUDE,
                        HealthConstants.UvExposure.ALTITUDE,
                        HealthConstants.UvExposure.ACCURACY
                },
                mListenerUvExposure
        );
    }

    /** Starts the database query for a specific {@link HealthConstants}
     *
     * @param pStatTime     Earliest time of measurement
     * @param pEndTime      Latest time of measurement
     * @param hcStartTime   Enum for start time
     * @param hcHDT
     * @param hcString      Array of requestet attributes
     * @param pmListener    Callback function for results
     */
    private void readHealthConstant(long pStatTime, long pEndTime, String hcStartTime, String hcHDT, String[] hcString, HealthResultHolder.ResultListener<ReadResult> pmListener) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        Filter filter = Filter.and(Filter.greaterThanEquals(hcStartTime, pStatTime),
                Filter.lessThan(hcStartTime, pEndTime));

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(hcHDT)
                .setProperties(hcString)
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(pmListener);
        } catch (Exception e) {
            Log.e(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
        }
    }

    /** Starts the database query for a specific {@link HealthConstants}
     *
     * @param filter        Filter that is used in query
     * @param hcHDT
     * @param hcString      Array of requestet attributes
     * @param pmListener    Callback function for results
     */
    private void readHealthConstantWithFilter(Filter filter, String hcHDT, String[] hcString, HealthResultHolder.ResultListener<ReadResult> pmListener) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(hcHDT)
                .setProperties(hcString)
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(pmListener);
        } catch (Exception e) {
            Log.e(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
        }
    }

    /** Callback for  {@link HealthConstants.StepCount}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerStepCount = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "StepCount").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.StepCount.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.StepCount.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.StepCount.TIME_OFFSET))).
                                add("COUNT", c.getInt(c.getColumnIndex(HealthConstants.StepCount.COUNT))).
                                add("DISTANCE", c.getFloat(c.getColumnIndex(HealthConstants.StepCount.DISTANCE))).
                                add("CALORIE", c.getFloat(c.getColumnIndex(HealthConstants.StepCount.CALORIE))).
                                add("SPEED", c.getFloat(c.getColumnIndex(HealthConstants.StepCount.SPEED))).
                                add("SAMPLE_POSITION_TYPE", c.getInt(c.getColumnIndex(HealthConstants.StepCount.SAMPLE_POSITION_TYPE))).
                                add("DEVICE_UUID", c.getLong(c.getColumnIndex(HealthConstants.StepCount.DEVICE_UUID))).
                                add("UUID", c.getLong(c.getColumnIndex(HealthConstants.StepCount.UUID))).
                                add("CREATE_TIME", c.getLong(c.getColumnIndex(HealthConstants.StepCount.CREATE_TIME))).
                                add("UPDATE_TIME", c.getLong(c.getColumnIndex(HealthConstants.StepCount.UPDATE_TIME)))
                        );

                    }
                }

            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link com.samsung.shealth.step_daily_trend}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerStepCountTrend = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    long addMS = (24*3600 - 1) * 1000;
                    while (c.moveToNext()) {
                        long day_time = c.getLong(c.getColumnIndex("day_time"));
                        // get date at time 23:59:59
                        // day_time + (23*3600 + 59*60 + 59) * 1000
                        long end_time = day_time + addMS;
                        
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "StepCountTrend").
                                add("START_TIME", day_time).
                                add("END_TIME", end_time).
                                add("TIME_OFFSET", 0).
                                add("COUNT", c.getInt(c.getColumnIndex("count"))).
                                add("DISTANCE", c.getFloat(c.getColumnIndex("distance"))).
                                add("CALORIE", c.getFloat(c.getColumnIndex("calorie"))).
                                add("SPEED", c.getFloat(c.getColumnIndex("speed"))).
                                add("SAMPLE_POSITION_TYPE", 0).
                                add("DEVICE_UUID", c.getLong(c.getColumnIndex("deviceuuid"))).
                                add("UUID", c.getLong(c.getColumnIndex("datauuid"))).
                                add("CREATE_TIME", c.getLong(c.getColumnIndex("create_time"))).
                                add("UPDATE_TIME", c.getLong(c.getColumnIndex("update_time")))
                        );

                    }
                }

            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.Exercise}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerExercise = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "Sleep").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.Exercise.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.Exercise.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.Exercise.TIME_OFFSET))).
                                add("CALORIE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.CALORIE))).
                                add("DURATION", c.getLong(c.getColumnIndex(HealthConstants.Exercise.DURATION))).
                                add("EXERCISE_TYPE", c.getInt(c.getColumnIndex(HealthConstants.Exercise.EXERCISE_TYPE))).
                                add("EXERCISE_CUSTOM_TYPE", c.getInt(c.getColumnIndex(HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE))).
                                add("DISTANCE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.DISTANCE))).
                                add("ALTITUDE_GAIN", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.ALTITUDE_GAIN))).
                                add("ALTITUDE_LOSS", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.ALTITUDE_LOSS))).
                                add("COUNT", c.getInt(c.getColumnIndex(HealthConstants.Exercise.COUNT))).
                                add("COUNT_TYPE", c.getInt(c.getColumnIndex(HealthConstants.Exercise.COUNT_TYPE))).
                                add("MAX_SPEED", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_SPEED))).
                                add("MEAN_SPEED", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_SPEED))).
                                add("MAX_CALORICBURN_RATE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_CALORICBURN_RATE))).
                                add("MEAN_CALORICBURN_RATE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_CALORICBURN_RATE))).
                                add("MAX_CADENCE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_CADENCE))).
                                add("MEAN_CADENCE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_CADENCE))).
                                add("MAX_HEART_RATE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_HEART_RATE))).
                                add("MEAN_HEART_RATE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_HEART_RATE))).
                                add("MIN_HEART_RATE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MIN_HEART_RATE))).
                                add("MAX_ALTITUDE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_ALTITUDE))).
                                add("MIN_ALTITUDE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MIN_ALTITUDE))).
                                add("INCLINE_DISTANCE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.INCLINE_DISTANCE))).
                                add("DECLINE_DISTANCE", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.DECLINE_DISTANCE))).
                                add("MAX_POWER", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MAX_POWER))).
                                add("MEAN_POWER", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_POWER))).
                                add("MEAN_RPM", c.getFloat(c.getColumnIndex(HealthConstants.Exercise.MEAN_RPM)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.Sleep}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerSleep = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "Sleep").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.Sleep.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.Sleep.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.Sleep.TIME_OFFSET))).
                                add("DEVICE_UUID", c.getLong(c.getColumnIndex(HealthConstants.Sleep.DEVICE_UUID))).
                                add("UUID", c.getLong(c.getColumnIndex(HealthConstants.Sleep.UUID))).
                                add("CREATE_TIME", c.getLong(c.getColumnIndex(HealthConstants.Sleep.CREATE_TIME))).
                                add("UPDATE_TIME", c.getLong(c.getColumnIndex(HealthConstants.Sleep.UPDATE_TIME)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.SleepStage}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerSleepStage = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "Sleep").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.SleepStage.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.SleepStage.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.SleepStage.TIME_OFFSET))).
                                add("SLEEP_ID", c.getString(c.getColumnIndex(HealthConstants.SleepStage.SLEEP_ID))).
                                add("STAGE", c.getInt(c.getColumnIndex(HealthConstants.SleepStage.STAGE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.FoodIntake}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerFoodIntake = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "FoodIntake").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.FoodIntake.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.FoodIntake.TIME_OFFSET))).
                                add("CALORIE", c.getFloat(c.getColumnIndex(HealthConstants.FoodIntake.CALORIE))).
                                add("FOOD_INFO_ID", c.getString(c.getColumnIndex(HealthConstants.FoodIntake.FOOD_INFO_ID))).
                                add("AMOUNT", c.getFloat(c.getColumnIndex(HealthConstants.FoodIntake.AMOUNT))).
                                add("UNIT", c.getString(c.getColumnIndex(HealthConstants.FoodIntake.UNIT))).
                                add("NAME", c.getString(c.getColumnIndex(HealthConstants.FoodIntake.NAME))).
                                add("MEAL_TYPE", c.getInt(c.getColumnIndex(HealthConstants.FoodIntake.MEAL_TYPE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.WaterIntake}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerWaterIntake = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "WaterIntake").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.WaterIntake.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.WaterIntake.TIME_OFFSET))).
                                add("AMOUNT", c.getFloat(c.getColumnIndex(HealthConstants.WaterIntake.AMOUNT))).
                                add("UNIT_AMOUNT", c.getFloat(c.getColumnIndex(HealthConstants.WaterIntake.UNIT_AMOUNT)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.CaffeineIntake}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerCaffeineIntake = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "CaffeineIntake").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.CaffeineIntake.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.CaffeineIntake.TIME_OFFSET))).
                                add("AMOUNT", c.getFloat(c.getColumnIndex(HealthConstants.CaffeineIntake.AMOUNT))).
                                add("UNIT_AMOUNT", c.getString(c.getColumnIndex(HealthConstants.CaffeineIntake.UNIT_AMOUNT)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.HeartRate}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerHeartRate = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "HeartRate").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.HeartRate.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.HeartRate.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.HeartRate.TIME_OFFSET))).
                                add("HEART_RATE", c.getFloat(c.getColumnIndex(HealthConstants.HeartRate.HEART_RATE))).
                                add("HEART_BEAT_COUNT", c.getInt(c.getColumnIndex(HealthConstants.HeartRate.HEART_BEAT_COUNT)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.BodyTemperature}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerBodyTemperature = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "BodyTemperature").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.BodyTemperature.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.BodyTemperature.TIME_OFFSET))).
                                add("TEMPERATURE", c.getFloat(c.getColumnIndex(HealthConstants.BodyTemperature.TEMPERATURE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.BloodPressure}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerBloodPressure = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "BloodPressure").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.BloodPressure.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.BloodPressure.TIME_OFFSET))).
                                add("SYSTOLIC", c.getFloat(c.getColumnIndex(HealthConstants.BloodPressure.SYSTOLIC))).
                                add("DIASTOLIC", c.getFloat(c.getColumnIndex(HealthConstants.BloodPressure.DIASTOLIC))).
                                add("MEAN", c.getFloat(c.getColumnIndex(HealthConstants.BloodPressure.MEAN))).
                                add("PULSE", c.getInt(c.getColumnIndex(HealthConstants.BloodPressure.PULSE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.BloodGlucose}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerBloodGlucose = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "BloodGlucose").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.BloodGlucose.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.BloodGlucose.TIME_OFFSET))).
                                add("GLUCOSE", c.getFloat(c.getColumnIndex(HealthConstants.BloodGlucose.GLUCOSE))).
                                add("MEAL_TIME", c.getLong(c.getColumnIndex(HealthConstants.BloodGlucose.MEAL_TIME))).
                                add("MEAL_TYPE", c.getInt(c.getColumnIndex(HealthConstants.BloodGlucose.MEAL_TYPE))).
                                add("MEASUREMENT_TYPE", c.getInt(c.getColumnIndex(HealthConstants.BloodGlucose.MEASUREMENT_TYPE))).
                                add("SAMPLE_SOURCE_TYPE", c.getInt(c.getColumnIndex(HealthConstants.BloodGlucose.SAMPLE_SOURCE_TYPE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.OxygenSaturation}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerOxygenSaturation = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "OxygenSaturation").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.OxygenSaturation.START_TIME))).
                                add("END_TIME", c.getLong(c.getColumnIndex(HealthConstants.OxygenSaturation.END_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.OxygenSaturation.TIME_OFFSET))).
                                add("SPO2", c.getFloat(c.getColumnIndex(HealthConstants.OxygenSaturation.SPO2))).
                                add("HEART_RATE", c.getFloat(c.getColumnIndex(HealthConstants.OxygenSaturation.HEART_RATE)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.HbA1c}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerHbA1c = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "HbA1c").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.HbA1c.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.HbA1c.TIME_OFFSET))).
                                add("HBA1C", c.getFloat(c.getColumnIndex(HealthConstants.HbA1c.HBA1C)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.AmbientTemperature}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerAmbientTemperature = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "AmbientTemperature").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.AmbientTemperature.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.AmbientTemperature.TIME_OFFSET))).
                                add("TEMPERATURE", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.TEMPERATURE))).
                                add("HUMIDITY", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.HUMIDITY))).
                                add("LATITUDE", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.LATITUDE))).
                                add("LONGITUDE", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.LONGITUDE))).
                                add("ALTITUDE", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.ALTITUDE))).
                                add("ACCURACY", c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.ACCURACY)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

    /** Callback for  {@link HealthConstants.UvExposure}
     *
     */
    private final HealthResultHolder.ResultListener<ReadResult> mListenerUvExposure = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            Cursor c = null;
            JsonArrayBuilder array = Json.createArrayBuilder();

            try {
                c = result.getResultCursor();

                if (c != null) {
                    while (c.moveToNext()) {
                        array.add(Json.createObjectBuilder().
                                add("TYPE", "UvExposure").
                                add("START_TIME", c.getLong(c.getColumnIndex(HealthConstants.UvExposure.START_TIME))).
                                add("TIME_OFFSET", c.getLong(c.getColumnIndex(HealthConstants.UvExposure.TIME_OFFSET))).
                                add("UV_INDEX", c.getFloat(c.getColumnIndex(HealthConstants.UvExposure.UV_INDEX))).
                                add("LATITUDE", c.getFloat(c.getColumnIndex(HealthConstants.UvExposure.LATITUDE))).
                                add("LONGITUDE", c.getFloat(c.getColumnIndex(HealthConstants.UvExposure.LONGITUDE))).
                                add("ALTITUDE", c.getFloat(c.getColumnIndex(HealthConstants.UvExposure.ALTITUDE))).
                                add("ACCURACY", c.getFloat(c.getColumnIndex(HealthConstants.UvExposure.ACCURACY)))
                        );
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            JsonArray jsonarr = array.build();
            Log.d(APP_TAG, jsonarr.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonarr.toString());
            //pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        }
    };

}
