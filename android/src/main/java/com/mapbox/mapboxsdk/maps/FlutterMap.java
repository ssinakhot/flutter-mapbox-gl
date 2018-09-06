package com.mapbox.mapboxsdk.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.util.Base64;
import android.util.Log;

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.ProjectedMeters;
import com.mapbox.mapboxsdk.maps.renderer.surfacetexture.SurfaceTextureMapRenderer;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.renderer.MapRenderer;
import com.mapbox.mapboxsdk.net.ConnectivityReceiver;
import com.mapbox.mapboxsdk.storage.FileSource;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.HeatmapLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyValue;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.net.URL;

public class FlutterMap implements NativeMapView.ViewCallback, MapView.OnMapChangedListener {
  private final Context context;
  private final NativeMapView nativeMapView;
  private final MapRenderer mapRenderer;
  private int width;
  private int height;

  public FlutterMap(Context context, MapboxMapOptions options, SurfaceTexture surfaceTexture, int width, int height) {
    this.context = context;
    this.width = width;
    this.height = height;

    String localFontFamily = options.getLocalIdeographFontFamily();
    boolean translucentSurface = options.getTranslucentTextureSurface();
    mapRenderer = new SurfaceTextureMapRenderer(context, surfaceTexture, width, height, localFontFamily,
        translucentSurface);

    nativeMapView = new NativeMapView(context, this, mapRenderer);
    nativeMapView.addOnMapChangedListener(this);
    nativeMapView.setStyleUrl(options.getStyle());
    nativeMapView.resizeView(width, height);
    nativeMapView.setReachability(ConnectivityReceiver.instance(context).isConnected(context));
    nativeMapView.update();

    CameraPosition cameraPosition = options.getCamera();
    if (cameraPosition != null) {
      nativeMapView.jumpTo(cameraPosition.bearing, cameraPosition.target, cameraPosition.tilt, cameraPosition.zoom);
    }
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public Bitmap getViewContent() {
    return null;
  }

  @Override
  public void onMapChanged(int change) {

  }

  public void onStart() {
    ConnectivityReceiver.instance(context).activate();
    FileSource.getInstance(context).activate();

    mapRenderer.onStart();
  }

  public void onResume() {
    mapRenderer.onResume();
  }

  public void onPause() {
    mapRenderer.onPause();
  }

  public void onStop() {
    mapRenderer.onStop();

    ConnectivityReceiver.instance(context).deactivate();
    FileSource.getInstance(context).deactivate();
  }

  public void onDestroy() {
    // null when destroying an activity programmatically mapbox-navigation-android/issues/503
    nativeMapView.destroy();
    mapRenderer.onDestroy();
  }

  public void setStyleUrl(String styleUrl){
    nativeMapView.setStyleUrl(styleUrl);
  }

  public String getStyleUrl(){
    return nativeMapView.getStyleUrl();
  }

  public void setStyleJson(String styleJson) {
    nativeMapView.setStyleJson(styleJson);
  }

  public String getStyleJson(){
    return nativeMapView.getStyleJson();
  }

  public void moveBy(double dx, double dy, long duration) {
    nativeMapView.moveBy(dx, dy, duration);
  }

  public void easeTo(CameraPosition cameraPosition, int duration, boolean easingInterpolator) {
    nativeMapView.easeTo(cameraPosition.bearing, cameraPosition.target, duration, cameraPosition.tilt,
        cameraPosition.zoom, easingInterpolator);
  }

  public void flyTo(CameraPosition cameraPosition, int duration) {
    nativeMapView.flyTo(cameraPosition.bearing, cameraPosition.target, duration, cameraPosition.tilt,
        cameraPosition.zoom);
  }

  public void jumpTo(CameraPosition cameraPosition) {
    nativeMapView.jumpTo(cameraPosition.bearing, cameraPosition.target, cameraPosition.tilt, cameraPosition.zoom);
  }

  public double getMetersPerPixelAtLatitude(double latitude){
    return nativeMapView.getMetersPerPixelAtLatitude(latitude);
  }

  public ProjectedMeters getProjecteMeters(LatLng latLng){
    return nativeMapView.projectedMetersForLatLng(latLng);
  }

  public LatLng getLatLng(ProjectedMeters projectedMeters){
    return nativeMapView.latLngForProjectedMeters(projectedMeters);
  }

  public LatLng getLatLng(PointF screenPoint){
    return nativeMapView.latLngForPixel(screenPoint);
  }

  public PointF getScreenPoint(LatLng latLng){
    return nativeMapView.pixelForLatLng(latLng);
  }

  public double getMinZoom() {
    return nativeMapView.getMinZoom();
  }

  public void setMinZoom(double zoom) {
    nativeMapView.setMinZoom(zoom);
  }

  public double getMaxZoom() {
    return nativeMapView.getMaxZoom();
  }

  public void setMaxZoom(double zoom) {
    nativeMapView.setMaxZoom(zoom);
  }

  public double getZoom() {
    return nativeMapView.getZoom();
  }

  public void zoom(double zoom, PointF focalPoint, long duration) {
    nativeMapView.setZoom(zoom, focalPoint, duration);
  }

  public void addMarker(String title, String snippet, LatLng latLng) {
    IconFactory iconFactory = IconFactory.getInstance(this.context);
    Icon icon = iconFactory.defaultMarker();
    MarkerOptions options = new MarkerOptions();
    options.icon(icon);
    options.position(latLng);
    options.title(title);
    options.snippet(snippet);
    nativeMapView.addMarker(new Marker(options));
  }

  public void addPolyline(LatLng[] points, Double width, String color) {
    PolylineOptions polylineOptions = new PolylineOptions();
    polylineOptions.add(points);
    polylineOptions.width(width.floatValue());
    polylineOptions.color(Color.parseColor(color));
    nativeMapView.addPolyline(polylineOptions.getPolyline());
  }

  public void addPolygon(LatLng[] points, String strokeColor, String fillColor) {
    PolygonOptions polygonOptions = new PolygonOptions();
    polygonOptions.add(points);
    polygonOptions.strokeColor(Color.parseColor(strokeColor));
    polygonOptions.fillColor(Color.parseColor(fillColor));
    nativeMapView.addPolygon(polygonOptions.getPolygon());
  }

  public void addGeoJsonSourceByUrl(String sourceId, URL url, GeoJsonOptions options) {
    nativeMapView.addSource(
        new GeoJsonSource(sourceId, url, options)
    );
  }

  public void addGeoJsonSource(String sourceId, String geoJson, GeoJsonOptions options) {
    nativeMapView.addSource(
        new GeoJsonSource(sourceId, geoJson, options)
    );
  }

  public void addCircleLayer(String layerId, String sourceId, PropertyValue<?>[] propertyValues, Expression filter) {
    CircleLayer circleLayer = new CircleLayer(layerId, sourceId);
    if (propertyValues != null)
      circleLayer.setProperties(propertyValues);
    if (filter != null)
      circleLayer.setFilter(filter);
    nativeMapView.addLayer(circleLayer);
  }

  public void addSymbolLayer(String layerId, String sourceId, PropertyValue<?>[] propertyValues, Expression filter) {
    SymbolLayer symbolLayer = new SymbolLayer(layerId, sourceId);
    if (propertyValues != null)
      symbolLayer.setProperties(propertyValues);
    if (filter != null)
      symbolLayer.setFilter(filter);
    nativeMapView.addLayer(symbolLayer);
  }

  public void addHeatmapLayer(String layerId, String sourceId, Integer maxZoom, PropertyValue<?>[] propertyValues, Expression filter) {
    HeatmapLayer heatmapLayer = new HeatmapLayer(layerId, sourceId);
    if (maxZoom != null)
      heatmapLayer.setMaxZoom(maxZoom);
    if (propertyValues != null)
      heatmapLayer.setProperties(propertyValues);
    if (filter != null)
      heatmapLayer.setFilter(filter);
    nativeMapView.addLayer(heatmapLayer);
  }

  public void addImage(String imageName, String base64Image) {
    byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    nativeMapView.addImage(imageName, image);
  }
}
