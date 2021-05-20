import React from 'react';
import { ResponsiveLine } from '@nivo/line';
import PropTypes from 'prop-types';

const COLORS = ['#0088ce', '#6e1e78', '#a1006b', '#cd0037', '#e05206', '#ffb612', '#82be00', '#d2e100', '#009aa6', '#333'];

// Format GOC Curves to NIVO format
const parseCourbeGOC = (label, color, data) => {
  const regex = /'/g;
  const curveJson = JSON.parse(data.replace(regex, '"'));
  const curveFormatted = [];
  Object.keys(curveJson).forEach((key) => {
    curveFormatted.push({ x: key, y: curveJson[key] });
  });

  const curveFormattedSorted = curveFormatted.sort(
    (a, b) => a.x.localeCompare(b.x, undefined, { numeric: true }),
  );

  return {
    id: label,
    color,
    data: curveFormattedSorted,
  };
};

// Choose cyclic color for curves
const curveColor = (index) => {
  const indexShort = index % COLORS.length;
  return COLORS[indexShort];
};

export default function TrainCompoDetailsCurve(props) {
  const { data } = props;
  const curves = data.map((curve, index) => parseCourbeGOC(
    curve.profil_mode, curveColor(index), curve.referencegoc.courbeeffortvitesse,
  ));

  return (
    <ResponsiveLine
      data={curves}
      margin={{
        top: 20,
        right: 20,
        bottom: 50,
        left: 70,
      }}
      xScale={{
        type: 'linear',
        min: 'auto',
        max: 'auto',
      }}
      yScale={{
        type: 'linear',
        min: 0,
        max: 'auto',
      }}
      curve="linear"
      axisTop={null}
      axisRight={null}
      axisBottom={{
        orient: 'bottom',
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: 'km/h',
        legendOffset: 36,
        legendPosition: 'middle',
      }}
      axisLeft={{
        orient: 'left',
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: 'kN/t',
        legendOffset: -65,
        legendPosition: 'middle',
      }}
      colors={{ datum: 'color' }}
      lineWidth={1}
      enablePoints={false}
      useMesh
    />
  );
}

TrainCompoDetailsCurve.propTypes = {
  data: PropTypes.array.isRequired,
};
