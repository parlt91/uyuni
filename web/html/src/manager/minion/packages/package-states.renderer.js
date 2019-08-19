import React from 'react';
import PackageStates from './package-states';
import SpaRenderer from "core/spa/spa-renderer";

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.minion = window.pageRenderers.minion || {};
window.pageRenderers.minion.packages = window.pageRenderers.minion.packages || {};
window.pageRenderers.minion.packages.renderer = (id, {serverId}) => {
  SpaRenderer.renderNavigationReact(
    <PackageStates
      serverId={serverId}
    />,
    document.getElementById(id)
  );
};
