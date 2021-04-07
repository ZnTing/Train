import React, { useCallback, useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useParams } from 'react-router-dom';
import ReactMapGL, { AttributionControl, Layer, ScaleControl, Source } from 'react-map-gl';
import { isEqual } from 'lodash';

import { selectZone } from 'reducers/editor';
import { updateViewport } from 'reducers/map';
import { getGeoJSONRectangle } from 'utils/helpers';
import { KeyDownMapController } from 'utils/mapboxHelper';
import osmBlankStyle from 'common/Map/Layers/osmBlankStyle';

import colors from 'common/Map/Consts/colors';
import 'common/Map/Map.scss';

/* Main data & layers */
import Background from 'common/Map/Layers/Background';
import OSM from 'common/Map/Layers/OSM';
import Hillshade from 'common/Map/Layers/Hillshade';
import Platform from 'common/Map/Layers/Platform';
import TracksGeographic from 'common/Map/Layers/TracksGeographic';
import EditorZone from 'common/Map/Layers/EditorZone';

const SELECTION_ZONE_STYLE = {
  type: 'line',
  paint: {
    'line-dasharray': [3, 3],
    'line-opacity': 0.8,
  },
};

const SelectZone = () => {
  const { mapStyle, viewport } = useSelector((state) => state.map);
  const { urlLat, urlLon, urlZoom, urlBearing, urlPitch } = useParams();
  const dispatch = useDispatch();
  const updateViewportChange = useCallback((value) => dispatch(updateViewport(value, '/editor')), [
    dispatch,
  ]);

  const [firstCorner, setFirstCorner] = useState(null);
  const [secondCorner, setSecondCorner] = useState(null);
  const geoJSON =
    firstCorner && secondCorner ? getGeoJSONRectangle(firstCorner, secondCorner) : null;

  /* Interactions */
  const getCursor = () => 'crosshair';
  const onClick = useCallback(
    (event) => {
      const corner = event.lngLat;
      if (!firstCorner) {
        setFirstCorner(corner);
        setSecondCorner(corner);
      } else if (isEqual(firstCorner, corner)) {
        setFirstCorner(null);
        setSecondCorner(null);
      } else {
        const zone = [firstCorner, corner];
        setFirstCorner(null);
        setSecondCorner(null);
        dispatch(selectZone(...zone));
      }
    },
    [firstCorner]
  );
  const onMove = useCallback(
    (event) => {
      if (firstCorner) {
        setSecondCorner(event.lngLat);
      }
    },
    [firstCorner]
  );
  const onKeyDown = useCallback(
    (event) => {
      if (event.key === 'Escape') {
        setFirstCorner(null);
        setSecondCorner(null);
      }
    },
    [firstCorner]
  );

  /* Custom controller for keyboard handling */
  const [mapController] = useState(new KeyDownMapController(onKeyDown));

  useEffect(() => {
    mapController.setHandler(onKeyDown);
  }, [onKeyDown]);

  useEffect(() => {
    if (urlLat) {
      updateViewportChange({
        ...viewport,
        latitude: parseFloat(urlLat),
        longitude: parseFloat(urlLon),
        zoom: parseFloat(urlZoom),
        bearing: parseFloat(urlBearing),
        pitch: parseFloat(urlPitch),
      });
    }
  }, []);

  return (
    <ReactMapGL
      {...viewport}
      width="100%"
      height="100%"
      getCursor={getCursor}
      mapStyle={osmBlankStyle}
      onViewportChange={updateViewportChange}
      attributionControl={false}
      onClick={onClick}
      onMouseMove={onMove}
      controller={mapController}
      touchRotate
      asyncRender
    >
      <AttributionControl
        className="attribution-control"
        customAttribution="©SNCF/DGEX Solutions"
      />
      <ScaleControl
        maxWidth={100}
        unit="metric"
        style={{
          left: 20,
          bottom: 20,
        }}
      />

      {/* OSM layers */}
      <Background colors={colors[mapStyle]} />
      <OSM mapStyle={mapStyle} />
      <Hillshade mapStyle={mapStyle} />
      <Platform colors={colors[mapStyle]} />

      {/* Chartis layers */}
      <TracksGeographic colors={colors[mapStyle]} />

      {/* Selection rectangle */}
      <EditorZone />
      {geoJSON && (
        <Source type="geojson" data={geoJSON}>
          <Layer {...SELECTION_ZONE_STYLE} />
        </Source>
      )}
    </ReactMapGL>
  );
};

export default SelectZone;
