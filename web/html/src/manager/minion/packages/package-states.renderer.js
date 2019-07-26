import React from 'react';
import ReactDOM from 'react-dom';
import PackageStates from './package-states';

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.minion = window.pageRenderers.minion || {};
window.pageRenderers.minion.packages = window.pageRenderers.minion.packages || {};
window.pageRenderers.minion.packages.renderer = (id, {serverId}) => {
  ReactDOM.render(
    <PackageStates
      serverId={serverId}
    />,
    document.getElementById(id),
  );
};
