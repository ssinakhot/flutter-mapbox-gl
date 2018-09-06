package com.mapbox.flutter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.ProjectedMeters;
import com.mapbox.mapboxsdk.maps.FlutterMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.view.FlutterView;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.division;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.gte;
import static com.mapbox.mapboxsdk.style.expressions.Expression.has;
import static com.mapbox.mapboxsdk.style.expressions.Expression.heatmapDensity;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.linear;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgba;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.toNumber;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleStrokeWidth;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapIntensity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.heatmapWeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textField;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.textSize;

/**
 * FlutterMapboxPlugin
 */
public class MapboxPlugin implements MethodCallHandler {

  private final FlutterView view;
  private Activity activity;
  private Registrar registrar;

  private static Map<Long, MapInstance> maps = new HashMap<>();

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private MapboxPlugin(Registrar registrar, FlutterView view, Activity activity) {
    this.registrar = registrar;
    this.view = view;
    this.activity = activity;

    activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            //                    mapInstance.map.onCreate(savedInstanceState);
          }
        }
      }

      @Override
      public void onActivityStarted(Activity activity) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            mapInstance.map.onStart();
          }
        }
      }

      @Override
      public void onActivityResumed(Activity activity) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            mapInstance.map.onResume();
          }
        }
      }

      @Override
      public void onActivityPaused(Activity activity) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            mapInstance.map.onPause();
          }
        }
      }

      @Override
      public void onActivityStopped(Activity activity) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            mapInstance.map.onStop();
          }
        }
      }

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (activity == MapboxPlugin.this.activity) {
          for (MapInstance mapInstance : maps.values()) {
            //                    mapInstance.map.onSaveInstanceState(outState);
          }
        }
      }

      @Override
      public void onActivityDestroyed(Activity activity) {
        if (activity == MapboxPlugin.this.activity) {
          //                  for (MapInstance mapInstance : maps.values()) {
          //                    mapInstance.release();
          //                  }
        }
      }
    });
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.mapbox/flutter_mapbox");
    channel.setMethodCallHandler(new MapboxPlugin(registrar, registrar.view(), registrar.activity()));
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
  @Override
  public void onMethodCall(MethodCall call, Result result) {
    switch (call.method) {
      case "create": {
        FlutterView.SurfaceTextureEntry surfaceTextureEntry = view.createSurfaceTexture();
        final int width = ((Number) call.argument("width")).intValue();
        final int height = ((Number) call.argument("height")).intValue();
        final MapboxMapOptions options = parseOptions((Map<String, Object>) call.argument("options"));
        SurfaceTexture surfaceTexture = surfaceTextureEntry.surfaceTexture();
        surfaceTexture.setDefaultBufferSize(width, height);
        FlutterMap mapView = new FlutterMap(activity, options, surfaceTexture, width, height);
        mapView.onStart();
        mapView.onResume();

        maps.put(surfaceTextureEntry.id(), new MapInstance(mapView, surfaceTextureEntry));
        Map<String, Object> reply = new HashMap<>();
        reply.put("textureId", surfaceTextureEntry.id());
        result.success(reply);
        break;
      }

      case "setStyleUrl": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          String styleUrl = stringParamOfCall(call, "styleUrl");
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.setStyleUrl(styleUrl);
        }
        result.success(null);
        break;
      }

      case "getStyleUrl": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          reply.put("styleUrl", mapInstance.map.getStyleUrl());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "setStyleJson": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          String styleUrl = stringParamOfCall(call, "styleJson");
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.setStyleJson(styleUrl);
        }
        result.success(null);
        break;
      }

      case "getStyleJson": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          reply.put("styleJson", mapInstance.map.getStyleJson());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "moveBy": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          double dx = doubleParamOfCall(call, "dx");
          double dy = doubleParamOfCall(call, "dy");
          long duration = longParamOfCall(call, "duration");
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.moveBy(dx, dy, duration);
        }
        result.success(null);
        break;
      }

      case "easeTo": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          CameraPosition cameraPosition = parseCamera(call.argument("camera"));
          int duration = intParamOfCall(call, "duration");
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.easeTo(cameraPosition, duration, true);
        }
        result.success(null);
        break;
      }

      case "flyTo": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          CameraPosition cameraPosition = parseCamera(call.argument("camera"));
          int duration = intParamOfCall(call, "duration");
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.flyTo(cameraPosition, duration);
        }
        result.success(null);
        break;
      }

      case "jumpTo": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          CameraPosition cameraPosition = parseCamera(call.argument("camera"));
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.map.jumpTo(cameraPosition);
        }
        result.success(null);
        break;
      }

      case "zoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          double zoom = doubleParamOfCall(call, "zoom");
          float x = floatParamOfCall(call, "x");
          float y = floatParamOfCall(call, "y");
          long duration = longParamOfCall(call, "duration");
          mapInstance.map.zoom(zoom, new PointF(x, y), duration);
        }
        result.success(null);
        break;
      }

      case "zoomBy": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          double zoomBy = doubleParamOfCall(call, "zoomBy");
          float x = floatParamOfCall(call, "x");
          float y = floatParamOfCall(call, "y");
          long duration = longParamOfCall(call, "duration");
          mapInstance.map.zoom(mapInstance.map.getZoom() + zoomBy, new PointF(x, y), duration);
        }
        result.success(null);
        break;
      }

      case "getMinZoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          reply.put("zoom", mapInstance.map.getMinZoom());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "setMinZoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          double zoom = doubleParamOfCall(call, "zoom");
          mapInstance.map.setMinZoom(zoom);
        }
        result.success(null);
        break;
      }

      case "getMaxZoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          reply.put("zoom", mapInstance.map.getMaxZoom());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "setMaxZoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          double zoom = doubleParamOfCall(call, "zoom");
          mapInstance.map.setMaxZoom(zoom);
        }
        result.success(null);
        break;
      }

      case "getZoom": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          reply.put("zoom", mapInstance.map.getZoom());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "getLatLngForPixel": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          LatLng latLng = mapInstance.map.getLatLng(screenPointParamOfCall(call));
          reply.put("lat", latLng.getLatitude());
          reply.put("lng", latLng.getLongitude());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "getPixelForLatLng": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          PointF pointF = mapInstance.map.getScreenPoint(latLngParamOfCall(call));
          reply.put("dx", pointF.x);
          reply.put("dy", pointF.y);
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "getProjectedMetersForLatLng": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          ProjectedMeters projectedMeters = mapInstance.map.getProjecteMeters(latLngParamOfCall(call));
          reply.put("northing", projectedMeters.getNorthing());
          reply.put("easting", projectedMeters.getEasting());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "getLatLngForProjectedMeters": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          LatLng latLng = mapInstance.map.getLatLng(projectedMetersPointParamOfCall(call));
          reply.put("lat", latLng.getLatitude());
          reply.put("lng", latLng.getLongitude());
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "getMetersPerPixelAtLatitude": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          Map<String, Object> reply = new HashMap<>();
          double latitude = doubleParamOfCall(call, "lat");
          reply.put("meters", mapInstance.map.getMetersPerPixelAtLatitude(latitude));
          result.success(reply);
        } else {
          result.success(null);
        }
        break;
      }

      case "addMarker": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String title = stringParamOfCall(call, "title");
          String snippet = stringParamOfCall(call, "snippet");
          LatLng latLng = latLngParamOfCall(call);
          mapInstance.map.addMarker(title, snippet, latLng);
        }
        result.success(null);
        break;
      }

      case "addPolyline": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          LatLng[] points = parseLatLngs(call.argument("points"));
          double width = doubleParamOfCall(call, "width");
          String color = stringParamOfCall(call, "color");
          mapInstance.map.addPolyline(points, width, color);
        }
        result.success(null);
        break;
      }

      case "addPolygon": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          LatLng[] points = parseLatLngs(call.argument("points"));
          String strokeColor = stringParamOfCall(call, "strokeColor");
          String fillColor = stringParamOfCall(call, "fillColor");
          mapInstance.map.addPolygon(points, strokeColor, fillColor);
        }
        result.success(null);
        break;
      }

      case "addGeoJsonSourceByUrl": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String sourceId = stringParamOfCall(call, "sourceId");
          String url = stringParamOfCall(call, "url");
          URL urlObj;
          try {
            urlObj = new URL(url);
          } catch (MalformedURLException malformedUrlException) {
            result.error("Check the URL " + malformedUrlException.getMessage(), "", null);
            break;
          }
          GeoJsonOptions options = geoJsonOptionsParamOfCall(call);
          mapInstance.map.addGeoJsonSourceByUrl(sourceId, urlObj, options);
        }
        result.success(null);
        break;
      }

      case "addGeoJsonSource": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String sourceId = stringParamOfCall(call, "sourceId");
          String geoJson = stringParamOfCall(call, "geoJson");
          GeoJsonOptions options = geoJsonOptionsParamOfCall(call);
          mapInstance.map.addGeoJsonSource(sourceId, geoJson, options);
        }
        result.success(null);
        break;
      }

      case "addCircleLayer": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String layerId = stringParamOfCall(call, "layerId");
          String sourceId = stringParamOfCall(call, "sourceId");
          PropertyValue<?>[] propertyValues = circlePropertiesParamOfCall(call);
          Expression filter = filterExpressionParamOfCall(call);
          mapInstance.map.addCircleLayer(layerId, sourceId, propertyValues, filter);
        }
        result.success(null);
        break;
      }

      case "addSymbolLayer": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String layerId = stringParamOfCall(call, "layerId");
          String sourceId = stringParamOfCall(call, "sourceId");
          PropertyValue<?>[] propertyValues = symbolPropertiesParamOfCall(call);
          Expression filter = filterExpressionParamOfCall(call);
          mapInstance.map.addSymbolLayer(layerId, sourceId, propertyValues, filter);
        }
        result.success(null);
        break;
      }

      case "addTextLayer": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String layerId = stringParamOfCall(call, "layerId");
          String sourceId = stringParamOfCall(call, "sourceId");
          PropertyValue<?>[] propertyValues = textPropertiesParamOfCall(call);
          Expression filter = filterExpressionParamOfCall(call);
          mapInstance.map.addSymbolLayer(layerId, sourceId, propertyValues, filter);
        }
        result.success(null);
        break;
      }

      case "addHeatmapLayer": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String layerId = stringParamOfCall(call, "layerId");
          String sourceId = stringParamOfCall(call, "sourceId");
          Integer maxZoom = intParamOfCall(call, "maxZoom");
          PropertyValue<?>[] propertyValues = heatmapPropertiesParamOfCall(call);
          Expression filter = filterExpressionParamOfCall(call);
          mapInstance.map.addHeatmapLayer(layerId, sourceId, maxZoom, propertyValues, filter);
        }
        result.success(null);
        break;
      }

      case "addImage": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapInstance = maps.get(textureId);
          String imageName = stringParamOfCall(call, "imageName");
          String base64Image = stringParamOfCall(call, "base64Image");
          mapInstance.map.addImage(imageName, base64Image);
        }
        result.success(null);
        break;
      }

      case "dispose": {
        long textureId = textureIdOfCall(call);
        if (maps.containsKey(textureId)) {
          MapInstance mapHolder = maps.get(textureId);
          mapHolder.release();
          maps.remove(textureId);
        } else {
          result.success(null);
        }
        break;
      }
      default:
        result.notImplemented();
    }
  }

  private boolean booleanParamOfCall(MethodCall call, String param) {
    return Boolean.parseBoolean(call.argument(param));
  }

  private double doubleParamOfCall(MethodCall call, String param) {
    return ((Number) call.argument(param)).doubleValue();
  }

  private float floatParamOfCall(MethodCall call, String param) {
    return ((Number) call.argument(param)).floatValue();
  }

  private int intParamOfCall(MethodCall call, String param) {
    return ((Number) call.argument(param)).intValue();
  }

  private long longParamOfCall(MethodCall call, String param) {
    return ((Number) call.argument(param)).longValue();
  }

  private String stringParamOfCall(MethodCall call, String param) {
    return (String) call.argument(param);
  }

  private long textureIdOfCall(MethodCall call) {
    return ((Number) call.argument("textureId")).longValue();
  }

  private LatLng latLngParamOfCall(MethodCall call) {
    Double lat = call.argument("lat");
    Double lng = call.argument("lng");
    return new LatLng(lat, lng);
  }

  private PointF screenPointParamOfCall(MethodCall call) {
    Double x = call.argument("x");
    Double y = call.argument("y");
    return new PointF(x.floatValue(), y.floatValue());
  }

  private ProjectedMeters projectedMetersPointParamOfCall(MethodCall call) {
    double easting = call.argument("easting");
    double northing = call.argument("northing");
    return new ProjectedMeters(northing, easting);
  }

  private GeoJsonOptions geoJsonOptionsParamOfCall(MethodCall call) {
    Map<String, Object> geoJsonOptionsProperties = call.argument("geoJsonOptions");
    GeoJsonOptions options = new GeoJsonOptions();
    for (Map.Entry<String, Object> property : geoJsonOptionsProperties.entrySet()) {
      if (property.getValue() == null)
        continue;
      switch (property.getKey()) {
        case "enableClustering":
          options = options.withCluster((boolean)property.getValue());
          break;
        case "clusterMaxZoom":
          options = options.withClusterMaxZoom((Integer)property.getValue());
          break;
        case "clusterRadius":
          options = options.withClusterRadius((Integer)property.getValue());
          break;
      }
    }
    return options;
  }

  private Expression filterExpressionParamOfCall(MethodCall call) {
    Map<String, Object> filterProperties = call.argument("filterProperties");
    Integer lowerRange = (Integer) filterProperties.get("lowerRange");
    Integer upperRange = (Integer) filterProperties.get("upperRange");
    if (lowerRange == null && upperRange == null)
      return null;
    Expression pointCount = toNumber(get("point_count"));
    return upperRange == null
        ? all(has("point_count"), gte(pointCount, literal(lowerRange)))
        : all(has("point_count"), gt(pointCount, literal(lowerRange)), lt(pointCount, literal(upperRange)));
  }

  private PropertyValue<?>[] circlePropertiesParamOfCall(MethodCall call) {
    Map<String, Object> circleProperties = call.argument("circleProperties");
    List<PropertyValue<?>> properties = new ArrayList<>();
    for (Map.Entry<String, Object> property : circleProperties.entrySet()) {
      if (property.getValue() == null)
        continue;
      switch (property.getKey()) {
        case "color":
          properties.add(circleColor((String)property.getValue()));
          break;
        case "radius":
          properties.add(circleRadius(((Double)property.getValue()).floatValue()));
          break;
        case "strokeColor":
          properties.add(circleStrokeColor((String)property.getValue()));
          break;
        case "strokeWidth":
          properties.add(circleStrokeWidth(((Double)property.getValue()).floatValue()));
          break;
      }
    }
    return properties.toArray(new PropertyValue<?>[properties.size()]);
  }

  private PropertyValue<?>[] symbolPropertiesParamOfCall(MethodCall call) {
    Map<String, Object> symbolProperties = call.argument("symbolProperties");
    List<PropertyValue<?>> properties = new ArrayList<>();
    for (Map.Entry<String, Object> property : symbolProperties.entrySet()) {
      if (property.getValue() == null)
        continue;
      switch (property.getKey()) {
        case "iconImageName":
          properties.add(iconImage((String)property.getValue()));
          break;
        case "iconSize":
          properties.add(iconSize(
              division(
                  get("mag"),
                  literal((Number)property.getValue())
              )
          ));
          break;
        case "iconColor":
          properties.add(iconColor((String)property.getValue()));
          break;
      }
    }
    return properties.toArray(new PropertyValue<?>[properties.size()]);
  }

  private PropertyValue<?>[] textPropertiesParamOfCall(MethodCall call) {
    Map<String, Object> textProperties = call.argument("textProperties");
    List<PropertyValue<?>> properties = new ArrayList<>();
    for (Map.Entry<String, Object> property : textProperties.entrySet()) {
      if (property.getValue() == null)
        continue;
      switch (property.getKey()) {
        case "textField":
          if (property.getValue().equals("point_count")) {
            properties.add(textField(Expression.toString(get("point_count"))));
          }
          else {
            properties.add(textField((String)property.getValue()));
          }
          break;
        case "textSize":
          properties.add(textSize(((Double)property.getValue()).floatValue()));
          break;
        case "textColor":
          properties.add(textColor((String)property.getValue()));
          break;
        case "textIgnorePlacement":
          properties.add(textIgnorePlacement((Boolean)property.getValue()));
          break;
        case "textAllowOverlap":
          properties.add(textAllowOverlap((Boolean)property.getValue()));
          break;
      }
    }
    return properties.toArray(new PropertyValue<?>[properties.size()]);
  }

  private PropertyValue<?>[] heatmapPropertiesParamOfCall(MethodCall call) {
    Map<String, Object> heatmapProperties = call.argument("heatmapProperties");
    return new PropertyValue<?>[] {
        // Color ramp for heatmap.  Domain is 0 (low) to 1 (high).
        // Begin color ramp at 0-stop with a 0-transparancy color
        // to create a blur-like effect.
        heatmapColor(
            interpolate(
                linear(), heatmapDensity(),
                literal(0), rgba(33, 102, 172, 0),
                literal(0.2), rgb(103, 169, 207),
                literal(0.4), rgb(209, 229, 240),
                literal(0.6), rgb(253, 219, 199),
                literal(0.8), rgb(239, 138, 98),
                literal(1), rgb(178, 24, 43)
            )
        ),

        // Increase the heatmap weight based on frequency and property magnitude
        heatmapWeight(
            interpolate(
                linear(), get("mag"),
                stop(0, 0),
                stop(6, 1)
            )
        ),

        // Increase the heatmap color weight weight by zoom level
        // heatmap-intensity is a multiplier on top of heatmap-weight
        heatmapIntensity(
            interpolate(
                linear(), zoom(),
                stop(0, 1),
                stop(9, 3)
            )
        ),

        // Adjust the heatmap radius by zoom level
        heatmapRadius(
            interpolate(
                linear(), zoom(),
                stop(0, 2),
                stop(9, 20)
            )
        ),

        // Transition from heatmap to circle layer by zoom level
        heatmapOpacity(
            interpolate(
                linear(), zoom(),
                stop(7, 1),
                stop(9, 0)
            )
        )
    };
  }

  private LatLng[] parseLatLngs(Map<String, Object>[] jsonPoints) {
    ArrayList<LatLng> latLngPoints = new ArrayList<>();
    for (Map<String, Object> jsonPoint : jsonPoints) {
      latLngPoints.add(parseLatLng(jsonPoint));
    }
    return (LatLng[])latLngPoints.toArray();
  }

  private MapboxMapOptions parseOptions(Map<String, Object> options) {

    String style = (String) options.get("style");
    if (style == null) {
      style = Style.MAPBOX_STREETS;
    }
    MapboxMapOptions mapOptions = new MapboxMapOptions().styleUrl(style);

    Map<String, Object> camera = (Map<String, Object>) options.get("camera");
    if (camera != null) {
      mapOptions.camera(parseCamera(camera));
    }
    return mapOptions;
  }

  private CameraPosition parseCamera(Map<String, Object> camera) {
    CameraPosition.Builder cameraPosition = new CameraPosition.Builder();

    LatLng target = parseLatLng((Map<String, Object>) camera.get("target"));
    if (target != null) {
      cameraPosition.target(target);
    }

    Double zoom = (Double) camera.get("zoom");
    if (zoom != null) {
      cameraPosition.zoom(zoom);
    }

    Double bearing = (Double) camera.get("bearing");
    if (bearing != null) {
      cameraPosition.bearing(bearing);
    }

    Double tilt = (Double) camera.get("tilt");
    if (tilt != null) {
      cameraPosition.tilt(tilt);
    }

    return cameraPosition.build();
  }

  private LatLng parseLatLng(Map<String, Object> target) {
    if (target.containsKey("lat") && target.containsKey("lng")) {
      return new LatLng(((Number) target.get("lat")).doubleValue(), ((Number) target.get("lng")).doubleValue());
    }
    return null;
  }

  private static class MapInstance {
    FlutterMap map;
    FlutterView.SurfaceTextureEntry surfaceTextureEntry;

    MapInstance(FlutterMap map, FlutterView.SurfaceTextureEntry surfaceTextureEntry) {
      this.map = map;
      this.surfaceTextureEntry = surfaceTextureEntry;
    }

    void release() {
      if (map != null) {
        map.onPause();
        map.onDestroy();
        map = null;
      }

      if (surfaceTextureEntry != null) {
        surfaceTextureEntry.release();
        surfaceTextureEntry = null;
      }
    }
  }
}
