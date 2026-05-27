<template>
  <div class="vizhub-page">
    <div class="page-header entrance-hero">
      <div>
        <span class="hierarchy-eyebrow">{{ uiText.visualizationLayer }}</span>
        <h2>{{ $t('nav.analysis') }}</h2>
        <p>{{ uiText.headerDesc }}</p>
      </div>
      <div class="header-index">
        <span>{{ modules.length }}</span>
        <small>{{ uiText.modules }}</small>
      </div>
    </div>

    <div class="run-selector mb-6">
      <el-card shadow="never" class="selector-card">
        <div class="selector-row">
          <label>{{ uiText.trainingRun }}</label>
          <el-select v-model="selectedRunId" :placeholder="uiText.selectRun" @change="handleRunChange" clearable class="run-select">
            <el-option v-for="run in runs" :key="run.id" :label="displayRunLabel(run)" :value="run.id" />
          </el-select>
          <el-button type="primary" size="small" @click="fetchRuns" :loading="loading">{{ uiText.refresh }}</el-button>
        </div>
      </el-card>
    </div>

    <section class="workspace-band preview-band entrance-up">
      <div class="band-caption">
        <span>{{ uiText.overviewPreview }}</span>
        <strong>{{ $t('nav.analysis') }}</strong>
      </div>
      <div class="preview-stage">
        <div class="preview-panel curve-panel">
        <div class="panel-head">
          <div>
            <span>{{ uiText.trainingSignal }}</span>
            <strong>{{ uiText.lossAccuracy }}</strong>
          </div>
          <em>{{ dataStatusLabel }}</em>
        </div>

        <div
          class="chart-box curve-chart interactive-chart"
          style="height: 330px"
          @pointermove="handleOverviewPointer"
          @pointerleave="clearOverviewProbe"
        >
          <svg class="overview-svg" viewBox="0 0 640 260" preserveAspectRatio="none" :aria-label="uiText.lossAccuracy">
            <defs>
              <linearGradient id="lossPreviewFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#fb7185" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#fb7185" stop-opacity="0" />
              </linearGradient>
              <linearGradient id="accuracyPreviewFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#42e6a4" stop-opacity="0.26" />
                <stop offset="100%" stop-color="#42e6a4" stop-opacity="0" />
              </linearGradient>
            </defs>
            <rect class="plot-bg" :x="chart.left" :y="chart.top" :width="chartInnerWidth" :height="chartInnerHeight" rx="10" />
            <g class="chart-grid">
              <line
                v-for="line in gridLines"
                :key="line"
                :x1="chart.left"
                :x2="chart.width - chart.right"
                :y1="line"
                :y2="line"
              />
            </g>
            <g class="vertical-grid">
              <line
                v-for="tick in verticalTicks"
                :key="tick"
                :x1="tick"
                :x2="tick"
                :y1="chart.top"
                :y2="baseline"
              />
            </g>
            <path v-if="lossPoints.length" :d="lossAreaPath" fill="url(#lossPreviewFill)" />
            <path v-if="accuracyPoints.length" :d="accuracyAreaPath" fill="url(#accuracyPreviewFill)" />
            <path v-if="lossPoints.length" :d="lossPath" class="loss-line" fill="none" stroke="#fb7185" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" />
            <path v-if="accuracyPoints.length" :d="accuracyPath" class="accuracy-line" fill="none" stroke="#42e6a4" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" />
            <g class="curve-markers">
              <circle
                v-for="point in lossMarkers"
                :key="'loss-' + point.x"
                :cx="point.x"
                :cy="point.y"
                r="3.1"
                fill="#fb7185"
              />
              <circle
                v-for="point in accuracyMarkers"
                :key="'accuracy-' + point.x"
                :cx="point.x"
                :cy="point.y"
                r="3.1"
                fill="#42e6a4"
              />
            </g>
            <circle v-if="lossPoints.length" :cx="lastLossPoint.x" :cy="lastLossPoint.y" r="5.4" class="loss-dot" fill="#fb7185" stroke="#0d1118" stroke-width="2" />
            <circle v-if="accuracyPoints.length" :cx="lastAccuracyPoint.x" :cy="lastAccuracyPoint.y" r="5.4" class="accuracy-dot" fill="#42e6a4" stroke="#0d1118" stroke-width="2" />
            <g class="axis-labels">
              <text :x="chart.left" y="246">{{ uiText.step }} {{ curveData.steps[0] || 1 }}</text>
              <text :x="chart.width - chart.right - 58" y="246">{{ uiText.step }} {{ curveData.steps[curveData.steps.length - 1] || '--' }}</text>
            </g>
            <g v-if="overviewProbe" class="chart-probe">
              <line class="probe-line" :x1="overviewProbe.x" :x2="overviewProbe.x" :y1="chart.top" :y2="baseline" />
              <line class="probe-line faint" :x1="chart.left" :x2="chart.width - chart.right" :y1="overviewProbe.lossY" :y2="overviewProbe.lossY" />
              <circle :cx="overviewProbe.x" :cy="overviewProbe.lossY" r="6" class="probe-dot loss" />
              <circle :cx="overviewProbe.x" :cy="overviewProbe.accuracyY" r="6" class="probe-dot accuracy" />
              <g :transform="`translate(${overviewProbe.tooltipX} 34)`" class="svg-tooltip">
                <rect width="134" height="58" rx="8" />
              <text x="10" y="18">{{ uiText.step }} {{ overviewProbe.step }}</text>
                <text x="10" y="36">{{ uiText.lossShort }} {{ overviewProbe.loss }}</text>
                <text x="10" y="52">{{ uiText.accuracyShort }} {{ overviewProbe.accuracy }}%</text>
              </g>
            </g>
          </svg>
          <div class="chart-legend">
            <span><i class="loss-key"></i>{{ uiText.lossLegend }}</span>
            <span><i class="accuracy-key"></i>{{ uiText.accuracyLegend }}</span>
          </div>
          <div class="chart-explainer">{{ uiText.lossAccuracyExplain }}</div>
          <div v-if="!hasScalarData" class="chart-empty-state">
            <strong>{{ dataLoading ? uiText.loading : uiText.missing }}</strong>
            <span>{{ dataError || uiText.emptyScalars }}</span>
          </div>
        </div>
      </div>

      <div class="preview-side">
        <div class="metric-grid">
          <div v-for="metric in previewMetrics" :key="metric.label" class="metric-card">
            <span>{{ metric.label }}</span>
            <strong :style="{ color: metric.tone }">{{ metric.value }}</strong>
            <em>{{ metric.detail }}</em>
          </div>
        </div>

        <div class="preview-panel distribution-panel">
          <div class="panel-head compact">
            <div>
              <span>{{ uiText.distribution }}</span>
              <strong>{{ uiText.weights }}</strong>
            </div>
            <em>{{ histogramCounts.length ? uiText.live : uiText.missing }}</em>
          </div>
          <div
            class="chart-box distribution-chart interactive-chart"
            style="height: 178px"
            @pointermove="handleDistributionPointer"
            @pointerleave="clearDistributionProbe"
          >
            <svg class="distribution-svg" viewBox="0 0 320 160" preserveAspectRatio="none" aria-label="Weight distribution preview chart">
              <defs>
                <linearGradient id="barPreviewFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#42e6a4" stop-opacity="0.98" />
                  <stop offset="100%" stop-color="#67e8f9" stop-opacity="0.72" />
                </linearGradient>
              </defs>
              <rect x="18" y="16" width="288" height="116" rx="8" class="distribution-bg" />
              <line x1="18" x2="306" y1="132" y2="132" class="distribution-axis" />
              <rect
                v-for="bar in distributionBars"
                :key="bar.label"
                :x="bar.x"
                :y="bar.y"
                :width="bar.width"
                :height="bar.height"
                rx="4"
                :fill="bar.fill"
                :opacity="bar.opacity"
                :class="{ active: distributionProbe?.index === bar.index }"
              />
              <circle
                v-for="node in distributionNodes"
                :key="node.x"
                :cx="node.x"
                :cy="node.y"
                r="2.4"
                fill="#fb7185"
                opacity="0.84"
              />
              <polyline
                :points="distributionPolyline"
                fill="url(#barPreviewFill)"
                stroke="#fb7185"
                stroke-width="1.4"
                fill-opacity="0"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <g v-if="distributionProbe" class="chart-probe">
                <line class="probe-line" :x1="distributionProbe.x" :x2="distributionProbe.x" y1="16" y2="132" />
                <circle :cx="distributionProbe.x" :cy="distributionProbe.y" r="5.5" class="probe-dot loss" />
                <g :transform="`translate(${distributionProbe.tooltipX} 24)`" class="svg-tooltip compact">
                  <rect width="104" height="42" rx="8" />
                  <text x="9" y="17">{{ uiText.bin }} {{ distributionProbe.label }}</text>
                  <text x="9" y="33">{{ uiText.weight }} {{ distributionProbe.value }}</text>
                </g>
              </g>
            </svg>
            <div v-if="!histogramCounts.length" class="chart-empty-state compact">
              <strong>{{ uiText.missing }}</strong>
              <span>{{ uiText.emptyModule }}</span>
            </div>
          </div>
        </div>
      </div>
      </div>
    </section>

    <section class="workspace-band overview-metrics-band">
      <div class="band-caption compact">
        <span>{{ uiText.liveComponents }}</span>
        <strong>{{ uiText.signalFlow }}</strong>
      </div>
      <div class="signal-strip">
      <div
        v-for="signal in signalCards"
        :key="signal.label"
        class="signal-card interactive-card"
        @pointermove="handleSignalPointer($event, signal)"
        @pointerleave="clearSignalProbe"
      >
        <span>{{ signal.label }}</span>
        <strong>{{ signal.value }}</strong>
        <div class="sparkline">
          <i
            v-for="(point, pointIndex) in signal.points"
            :key="pointIndex"
            :class="{ active: signalProbe?.label === signal.label && signalProbe.index === pointIndex }"
            :style="{ height: point + '%', background: signal.color }"
          ></i>
        </div>
        <div v-if="signalProbe?.label === signal.label" class="dom-tooltip" :style="{ left: signalProbe.x + '%', top: signalProbe.y + '%' }">
          <strong>{{ signalProbe.value }}</strong>
          <span>{{ uiText.step }} {{ signalProbe.index + 1 }}</span>
        </div>
      </div>
      </div>

      <div class="component-preview">
      <div
        v-for="node in componentNodes"
        :key="node.name"
        class="component-node interactive-card"
        @pointermove="handleComponentPointer($event, node)"
        @pointerleave="clearComponentProbe"
      >
        <span>{{ node.stage }}</span>
        <strong>{{ node.name }}</strong>
        <div class="node-bars">
          <i
            v-for="(bar, barIndex) in node.bars"
            :key="barIndex"
            :class="{ active: componentProbe?.name === node.name && componentProbe.index === barIndex }"
            :style="{ height: bar + '%' }"
          ></i>
        </div>
        <div v-if="componentProbe?.name === node.name" class="dom-tooltip" :style="{ left: componentProbe.x + '%', top: componentProbe.y + '%' }">
          <strong>{{ componentProbe.value }}%</strong>
          <span>{{ node.stage }} / {{ componentProbe.index + 1 }}</span>
        </div>
      </div>
      </div>
    </section>

    <div class="viz-modules workspace-band module-band" :style="{ '--active-accent': activeModule.accent }">
      <div class="section-caption module-workbench-head">
        <div>
          <span>{{ uiText.moduleSwitcher }}</span>
          <strong>{{ uiText.visualModules }}</strong>
          <p>{{ activeAnalysisModeMeta.desc }}</p>
        </div>
        <div class="analysis-mode-switch compact" role="tablist" :aria-label="uiText.analysisModeSwitch">
          <button type="button" :class="{ active: analysisMode === 'oneToMany' }" @click="setAnalysisMode('oneToMany')">
            <b>{{ uiText.oneToMany }}</b>
            <small>{{ uiText.oneToManyShort }}</small>
          </button>
          <button type="button" :class="{ active: analysisMode === 'manyToMany' }" @click="setAnalysisMode('manyToMany')">
            <b>{{ uiText.manyToMany }}</b>
            <small>{{ uiText.manyToManyShort }}</small>
          </button>
          <i :style="{ transform: analysisMode === 'manyToMany' ? 'translateX(100%)' : 'translateX(0)' }"></i>
        </div>
      </div>
      <div class="module-lab">
        <aside class="module-preview-panel">
          <div class="module-preview-copy">
            <span>{{ activeModule.stage }}</span>
            <strong>{{ $t('nav.' + activeModule.key) }}</strong>
            <p>{{ activeModule.desc }}</p>
          </div>
          <div
            class="module-preview-visual interactive-chart"
            :style="{ '--probe-x': modulePointer.x + '%', '--probe-y': modulePointer.y + '%' }"
            :class="['preview-' + activeModule.preview, { 'is-switching': isModuleSwitching }]"
            @pointermove="handleModulePointer"
            @pointerleave="clearModuleProbe"
          >
            <div class="module-art-grid"></div>
            <svg class="module-art-svg" viewBox="0 0 360 220" preserveAspectRatio="none" aria-hidden="true">
              <path class="module-art-area" :d="moduleArtAreaPath" />
              <path class="module-art-path primary" :d="moduleArtPrimaryPath" />
              <path class="module-art-path secondary" :d="moduleArtSecondaryPath" />
              <rect
                v-for="bar in moduleArtBars"
                :key="bar.id"
                :x="bar.x"
                :y="bar.y"
                :width="bar.width"
                :height="bar.height"
                :rx="bar.radius"
                class="module-art-bar"
                :class="{ active: moduleProbe?.kind === 'bar' && moduleProbe.index === bar.index }"
              />
              <line
                v-for="edge in moduleArtEdges"
                :key="edge.id"
                :x1="edge.x1"
                :y1="edge.y1"
                :x2="edge.x2"
                :y2="edge.y2"
                class="module-art-edge"
              />
              <circle
                v-for="node in moduleArtNodes"
                :key="node.id"
                :cx="node.x"
                :cy="node.y"
                :r="node.r"
                class="module-art-node"
                :class="{ active: moduleProbe?.kind === 'node' && moduleProbe.index === node.index }"
              />
              <g v-if="moduleProbe" class="chart-probe module-svg-probe">
                <line class="probe-line" :x1="moduleProbe.svgX" :x2="moduleProbe.svgX" y1="24" y2="194" />
                <line class="probe-line faint" x1="24" x2="336" :y1="moduleProbe.svgY" :y2="moduleProbe.svgY" />
                <circle :cx="moduleProbe.svgX" :cy="moduleProbe.svgY" r="6" class="probe-dot accuracy" />
              </g>
            </svg>
            <div class="module-art-code">
              <span v-for="line in activeModule.lines" :key="line">{{ line }}</span>
            </div>
            <div v-if="!activeSnapshot.hasData" class="chart-empty-state module-empty-state">
              <strong>{{ uiText.missing }}</strong>
              <span>{{ activeSnapshot.summary }}</span>
            </div>
            <div v-if="moduleProbe" class="module-readout" :style="{ left: moduleProbe.x + '%', top: moduleProbe.y + '%' }">
              <strong>{{ moduleProbe.value }}</strong>
              <span>{{ moduleProbe.label }}</span>
            </div>
          </div>
          <div class="module-preview-meta">
            <span v-for="stat in activeModuleStats" :key="stat.label">
              <strong>{{ stat.value }}</strong>
              <em>{{ stat.label }}</em>
            </span>
          </div>
          <div class="module-analysis-strip">
            <div class="matrix-control-head">
              <span>{{ uiText.analysisModeEyebrow }}</span>
              <strong>{{ activeAnalysisModeMeta.title }}</strong>
            </div>
            <div class="analysis-smart-row">
              <button type="button" class="smart-select-action" @click="smartSelectAnalyzableModules">
                {{ uiText.smartSelectModules }}
              </button>
              <button v-if="selectedAnalysisModules.length" type="button" class="clear-select-action" @click="clearSelectedAnalysisModules">
                {{ uiText.clearSelectedModules }}
              </button>
            </div>
            <div class="analysis-module-ribbon">
              <button
                v-for="mod in selectedModuleChips"
                :key="mod.key"
                type="button"
                :style="{ '--module-accent': mod.accent }"
                @click="setActiveModule(mod.key)"
              >
                <span>{{ mod.stage }}</span>
                <b>{{ mod.title }}</b>
              </button>
              <em v-if="!selectedModuleChips.length">{{ uiText.noModulePicked }}</em>
            </div>
            <div class="matrix-action-stack analysis-action-row">
              <em>{{ analysisMode === 'oneToMany' ? oneToManySelectionSummary : matrixSelectionSummary }}</em>
              <el-button
                type="primary"
                :loading="analysisRunning"
                :disabled="analysisMode === 'oneToMany' ? !canRunOneToManyAnalysis : !canRunMatrixAnalysis"
                @click="analysisMode === 'oneToMany' ? runOneToManyAnalysis() : runMatrixAnalysis()"
              >
                {{ analysisMode === 'oneToMany' ? uiText.runOneToMany : uiText.runMatrix }}
              </el-button>
            </div>
          </div>
        </aside>

        <div class="module-grid-panel">
          <Transition name="matrix-result">
            <div v-if="analysisMode === 'manyToMany'" class="matrix-control-card matrix-run-picker">
              <div class="matrix-control-head">
                <span>{{ uiText.matrixModels }}</span>
                <strong>{{ selectedAnalysisRunKeys.length }} / {{ runs.length }}</strong>
              </div>
              <el-checkbox-group v-model="selectedAnalysisRunKeys" class="matrix-check-grid compact-runs">
                <el-checkbox
                  v-for="run in runs"
                  :key="runKey(run)"
                  :label="runKey(run)"
                  border
                >
                  <span class="matrix-run-label">{{ displayRunLabel(run) }}</span>
                  <em>{{ runTypeLabel(run.type) }} · {{ statusLabel(run.status) }}</em>
                </el-checkbox>
              </el-checkbox-group>
            </div>
          </Transition>
          <el-row :gutter="16">
            <el-col v-for="mod in modules" :key="mod.key" :xs="24" :sm="12" :md="8" :lg="6" class="mb-4">
              <el-card
                shadow="never"
                class="module-card cursor-pointer surface-hover"
                :class="{ active: activeModule.key === mod.key, selected: detailModule.key === mod.key, picked: isModuleSelectedForActiveMode(mod.key), analyzable: moduleSnapshot(mod.key).hasData, diving: transitioningModule?.key === mod.key }"
                :style="{ '--module-accent': mod.accent }"
                role="button"
                tabindex="0"
                @mouseenter="setActiveModule(mod.key)"
                @pointermove="handleModuleCardPointer($event, mod)"
                @pointerleave="clearMiniProbe"
                @focusin="setActiveModule(mod.key)"
                @click="openModule(mod)"
                @keydown.enter.prevent="openModule(mod)"
                @keydown.space.prevent="openModule(mod)"
              >
                <div class="module-topline">
                  <span>{{ mod.stage }}</span>
                  <el-icon><ArrowRight /></el-icon>
                </div>
                <div class="module-icon">
                  <el-icon :size="28"><component :is="mod.icon" /></el-icon>
                </div>
                <h3>{{ $t('nav.' + mod.key) }}</h3>
                <p>{{ mod.desc }}</p>
                <div class="module-mini-preview" :class="'preview-' + mod.preview">
                  <i
                    v-for="(bar, barIndex) in moduleMiniBars(mod)"
                    :key="barIndex"
                    :class="{ active: miniProbe?.key === mod.key && miniProbe.index === barIndex }"
                    :style="{ height: bar + '%' }"
                  ></i>
                </div>
                <div v-if="miniProbe?.key === mod.key" class="dom-tooltip mini-tooltip" :style="{ left: miniProbe.x + '%', top: miniProbe.y + '%' }">
                  <strong>{{ miniProbe.value }}</strong>
                  <span>{{ miniProbe.label }}</span>
                </div>
                <button
                  type="button"
                  class="module-select-toggle"
                  :class="{ picked: isModuleSelectedForActiveMode(mod.key), analyzable: moduleSnapshot(mod.key).hasData }"
                  :style="{ '--module-accent': mod.accent }"
                  @click.stop="toggleModuleSelected(mod.key)"
                  @keydown.enter.stop.prevent="toggleModuleSelected(mod.key)"
                  @keydown.space.stop.prevent="toggleModuleSelected(mod.key)"
                >
                  <span>{{ isModuleSelectedForActiveMode(mod.key) ? uiText.selectedForAnalysis : uiText.selectForAnalysis }}</span>
                  <b>{{ moduleSnapshot(mod.key).hasData ? uiText.canAnalyze : uiText.noDataShort }}</b>
                </button>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>

      <Transition name="detail-panel" mode="out-in">
        <section
          ref="detailPanelRef"
          :key="detailModule.key"
          class="module-detail-panel"
          :style="{ '--active-accent': detailModule.accent, '--probe-x': detailPointer.x + '%', '--probe-y': detailPointer.y + '%' }"
        >
          <div class="detail-head">
            <div>
              <span>{{ detailModule.stage }} {{ uiText.detail }}</span>
              <strong>{{ $t('nav.' + detailModule.key) }}</strong>
              <p>{{ detailModule.desc }}</p>
            </div>
            <div class="detail-stats">
              <span v-for="stat in detailModuleStats" :key="stat.label">
                <strong>{{ stat.value }}</strong>
                <em>{{ stat.label }}</em>
              </span>
            </div>
          </div>

          <div class="detail-body">
            <div
              class="detail-chart interactive-chart"
              @pointermove="handleDetailPointer"
              @pointerleave="clearDetailProbe"
            >
              <svg class="detail-svg" viewBox="0 0 720 360" preserveAspectRatio="none" aria-label="Visualization module detail chart">
                <defs>
                  <linearGradient id="detailFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stop-color="var(--active-accent)" stop-opacity="0.28" />
                    <stop offset="100%" stop-color="var(--active-accent)" stop-opacity="0" />
                  </linearGradient>
                </defs>
                <rect class="detail-plot-bg" x="34" y="24" width="652" height="276" rx="12" />
                <g class="detail-grid">
                  <line v-for="line in detailGridY" :key="'y' + line" x1="34" x2="686" :y1="line" :y2="line" />
                  <line v-for="line in detailGridX" :key="'x' + line" :x1="line" :x2="line" y1="24" y2="300" />
                </g>
                <path v-if="detailHasCurve" class="detail-area" :d="detailAreaPath" />
                <path v-if="detailHasCurve" class="detail-path primary" :d="detailPrimaryPath" />
                <path v-if="detailHasCurve" class="detail-path secondary" :d="detailSecondaryPath" />
                <rect
                  v-for="bar in detailBars"
                  :key="bar.id"
                  :x="bar.x"
                  :y="bar.y"
                  :width="bar.width"
                  :height="bar.height"
                  :rx="bar.radius"
                  class="detail-bar"
                  :class="{ active: detailProbe?.kind === 'bar' && detailProbe.index === bar.index }"
                />
                <line
                  v-for="edge in detailEdges"
                  :key="edge.id"
                  :x1="edge.x1"
                  :y1="edge.y1"
                  :x2="edge.x2"
                  :y2="edge.y2"
                  class="detail-edge"
                />
                <circle
                  v-for="node in detailNodes"
                  :key="node.id"
                  :cx="node.x"
                  :cy="node.y"
                  :r="node.r"
                  class="detail-node"
                  :class="{ active: detailProbe?.kind === 'node' && detailProbe.index === node.index }"
                />
                <g v-if="detailProbe" class="chart-probe detail-probe">
                  <line class="probe-line" :x1="detailProbe.svgX" :x2="detailProbe.svgX" y1="24" y2="300" />
                  <line class="probe-line faint" x1="34" x2="686" :y1="detailProbe.svgY" :y2="detailProbe.svgY" />
                  <circle :cx="detailProbe.svgX" :cy="detailProbe.svgY" r="6.5" class="probe-dot accuracy" />
                  <g :transform="`translate(${detailProbe.svgX > 560 ? detailProbe.svgX - 150 : detailProbe.svgX + 12} ${Math.max(34, detailProbe.svgY - 56)})`" class="svg-tooltip detail-tooltip">
                    <rect width="138" height="54" rx="8" />
                    <text x="10" y="18">{{ detailProbe.label }}</text>
                    <text x="10" y="37">{{ detailProbe.value }}</text>
                  </g>
                </g>
                <g class="axis-labels">
                  <text x="34" y="326">{{ detailAxisLabels[0] }}</text>
                  <text x="580" y="326">{{ detailAxisLabels[1] }}</text>
                </g>
              </svg>
              <div v-if="!detailSnapshot.hasData" class="chart-empty-state detail-empty-state">
                <strong>{{ uiText.missing }}</strong>
                <span>{{ detailSnapshot.summary }}</span>
              </div>
            </div>

            <aside class="detail-inspector">
              <div class="detail-readout">
                <span>{{ uiText.pointer }}</span>
                <strong>{{ detailProbe?.value || detailSummary.primary }}</strong>
                <em>{{ detailProbe?.label || detailSummary.secondary }}</em>
              </div>
              <div class="detail-lines">
                <span v-for="line in detailModule.lines" :key="line">{{ line }}</span>
              </div>
              <div class="detail-table">
                <div v-for="row in detailRows" :key="row.label">
                  <span>{{ row.label }}</span>
                  <strong>{{ row.value }}</strong>
                </div>
              </div>
            </aside>
          </div>
        </section>
      </Transition>

      <Transition name="matrix-result">
        <div v-if="activeAnalysisBatch?.results?.length" class="matrix-results embedded-results">
          <div class="matrix-result-head">
            <div>
              <span>{{ uiText.matrixResults }}</span>
              <strong>{{ activeAnalysisBatch.title }}</strong>
            </div>
            <em>{{ filteredAnalysisResults.length }} / {{ activeAnalysisBatch.results.length }} {{ uiText.matrixCells }}</em>
          </div>
          <div class="result-filter-panel">
            <div class="result-view-switch">
              <button type="button" :class="{ active: resultViewMode === 'flat' }" @click="setResultViewMode('flat')">{{ uiText.resultFlat }}</button>
              <button type="button" :class="{ active: resultViewMode === 'model' }" @click="setResultViewMode('model')">{{ uiText.resultByModel }}</button>
              <button type="button" :class="{ active: resultViewMode === 'module' }" @click="setResultViewMode('module')">{{ uiText.resultByModule }}</button>
            </div>
            <div class="result-filter-actions">
              <button type="button" class="smart-select-action" @click="selectAllResultFilters">{{ uiText.resultSelectAll }}</button>
              <button type="button" class="clear-select-action" @click="clearResultFilters">{{ uiText.resultClearFilters }}</button>
            </div>
            <div class="result-persist-actions">
              <em>{{ resultSelectionSummary }}</em>
              <button type="button" class="select-visible-action" @click="selectVisibleAnalysisResults">{{ uiText.selectVisibleResults }}</button>
              <button type="button" class="clear-select-action" @click="clearAnalysisResultSelection">{{ uiText.clearResultSelection }}</button>
              <button type="button" class="save-view-action" :disabled="!!resultPersistenceBusy" @click="saveAnalysisResults(false)">
                {{ resultPersistenceBusy === 'save-view' ? uiText.savingResults : uiText.saveVisibleResults }}
              </button>
              <button type="button" class="save-selected-action" :disabled="!!resultPersistenceBusy || !selectedAnalysisResultIds.length" @click="saveAnalysisResults(true)">
                {{ resultPersistenceBusy === 'save-selected' ? uiText.savingResults : uiText.saveSelectedResults }}
              </button>
              <button type="button" class="save-view-action strong" :disabled="!!resultPersistenceBusy" @click="viewSavePanelOpen = !viewSavePanelOpen">
                {{ resultPersistenceBusy === 'save-cloud-view' ? uiText.savingResults : uiText.saveViewPanel }}
              </button>
              <button type="button" class="import-chat-action" :disabled="!!resultPersistenceBusy" @click="importAnalysisResultsToChat">
                {{ resultPersistenceBusy === 'import-chat' ? uiText.importingToChat : uiText.importToAiChat }}
              </button>
            </div>
            <div v-if="viewSavePanelOpen" class="view-save-panel">
              <div class="view-save-head">
                <div>
                  <strong>{{ uiText.saveViewPanel }}</strong>
                  <span>{{ uiText.saveViewPanelDesc }}</span>
                </div>
                <button type="button" @click="viewSavePanelOpen = false">{{ uiText.close }}</button>
              </div>
              <div class="view-save-grid">
                <label>
                  <span>{{ uiText.saveTarget }}</span>
                  <select v-model="viewSaveTarget">
                    <option value="cloud">{{ uiText.saveTargetCloud }}</option>
                    <option value="local">{{ uiText.saveTargetLocal }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ uiText.saveContent }}</span>
                  <select v-model="viewSaveKind">
                    <option value="text">{{ uiText.saveContentText }}</option>
                    <option value="image">{{ uiText.saveContentImage }}</option>
                    <option value="both">{{ uiText.saveContentBoth }}</option>
                  </select>
                </label>
                <label>
                  <span>{{ uiText.saveFormat }}</span>
                  <select v-model="viewSaveFormat">
                    <option value="md">Markdown</option>
                    <option value="word">Word</option>
                    <option value="json">JSON</option>
                    <option value="svg">SVG</option>
                  </select>
                </label>
                <button type="button" :disabled="!!resultPersistenceBusy" @click="saveVisualViewWithOptions">
                  {{ resultPersistenceBusy === 'save-cloud-view' ? uiText.savingResults : uiText.executeSave }}
                </button>
              </div>
            </div>
            <div v-if="resultViewMode !== 'flat'" class="result-filter-chips">
              <button
                v-for="option in activeResultFilterOptions"
                :key="option.key"
                type="button"
                :class="{ active: isResultFilterSelected(option.key) }"
                @click="toggleResultFilter(option.key)"
              >
                <span>{{ option.label }}</span>
                <b>{{ option.count }}</b>
              </button>
            </div>
          </div>
          <div v-if="resultViewMode === 'flat'" class="matrix-result-grid">
            <article
              v-for="result in filteredAnalysisResults"
              :key="result.id"
              class="matrix-result-card"
              :class="{ empty: result.status !== 'ready', selected: isAnalysisResultSelected(result.id) }"
            >
              <button
                type="button"
                class="result-card-select"
                :class="{ active: isAnalysisResultSelected(result.id) }"
                @click.stop="toggleAnalysisResultSelection(result.id)"
              >
                {{ isAnalysisResultSelected(result.id) ? uiText.resultSelected : uiText.resultSelect }}
              </button>
              <div class="result-card-top">
                <span>{{ moduleTitle(result.moduleKey) }}</span>
                <strong>{{ result.modelName || result.runName }}</strong>
              </div>
              <div class="result-status-row">
                <el-tag :type="result.status === 'ready' ? 'success' : 'warning'" size="small">
                  {{ resultStatusLabel(result.status) }}
                </el-tag>
                <span v-if="result.latestStep !== null && result.latestStep !== undefined">{{ uiText.latestStep }} {{ result.latestStep }}</span>
              </div>
              <div class="result-score">
                <strong>{{ formatResultScore(result.score) }}</strong>
                <em>{{ result.recordCount }} {{ uiText.records }}</em>
              </div>
              <p>{{ localizedResultSummary(result) }}</p>
              <div class="ai-panel-slot">
                <span>{{ uiText.aiPanel }}</span>
                <div class="ai-panel-meta">
                  <em :class="panelStatusClass(result)">{{ panelStatusLabel(result) }}</em>
                  <b>{{ panelModelName(result) }}</b>
                </div>
                <strong>{{ localizedPanelTitle(result) }}</strong>
                <p>{{ localizedPanelInsight(result) }}</p>
                <div v-if="localizedRecommendations(result).length" class="ai-recommendations">
                  <b v-for="item in localizedRecommendations(result)" :key="item">{{ item }}</b>
                </div>
                <div class="ai-panel-actions">
                  <button
                    type="button"
                    class="model-action"
                    :disabled="isPanelRefreshing(result.id)"
                    @click="refreshModelAiPanel(result.id)"
                  >
                    {{ isModelPanelRefreshing(result.id) ? uiText.generatingModelPanel : uiText.generateModelPanel }}
                  </button>
                  <button
                    type="button"
                    class="rule-action"
                    :disabled="isPanelRefreshing(result.id)"
                    @click="refreshRuleAiPanel(result.id)"
                  >
                    {{ isRulePanelRefreshing(result.id) ? uiText.refreshingRulePanel : uiText.refreshRulePanel }}
                  </button>
                </div>
              </div>
            </article>
          </div>
          <div v-else class="result-group-stack">
            <section v-for="group in groupedAnalysisResults" :key="group.key" class="result-group-panel">
              <div class="result-group-head">
                <span>{{ resultViewMode === 'model' ? uiText.resultByModel : uiText.resultByModule }}</span>
                <strong>{{ group.label }}</strong>
                <em>{{ group.results.length }} {{ uiText.matrixCells }}</em>
              </div>
              <div class="matrix-result-grid">
                <article
                  v-for="result in group.results"
                  :key="result.id"
                  class="matrix-result-card"
                  :class="{ empty: result.status !== 'ready', selected: isAnalysisResultSelected(result.id) }"
                >
                  <button
                    type="button"
                    class="result-card-select"
                    :class="{ active: isAnalysisResultSelected(result.id) }"
                    @click.stop="toggleAnalysisResultSelection(result.id)"
                  >
                    {{ isAnalysisResultSelected(result.id) ? uiText.resultSelected : uiText.resultSelect }}
                  </button>
                  <div class="result-card-top">
                    <span>{{ moduleTitle(result.moduleKey) }}</span>
                    <strong>{{ result.modelName || result.runName }}</strong>
                  </div>
                  <div class="result-status-row">
                    <el-tag :type="result.status === 'ready' ? 'success' : 'warning'" size="small">
                      {{ resultStatusLabel(result.status) }}
                    </el-tag>
                    <span v-if="result.latestStep !== null && result.latestStep !== undefined">{{ uiText.latestStep }} {{ result.latestStep }}</span>
                  </div>
                  <div class="result-score">
                    <strong>{{ formatResultScore(result.score) }}</strong>
                    <em>{{ result.recordCount }} {{ uiText.records }}</em>
                  </div>
                  <p>{{ localizedResultSummary(result) }}</p>
                  <div class="ai-panel-slot">
                    <span>{{ uiText.aiPanel }}</span>
                    <div class="ai-panel-meta">
                      <em :class="panelStatusClass(result)">{{ panelStatusLabel(result) }}</em>
                      <b>{{ panelModelName(result) }}</b>
                    </div>
                    <strong>{{ localizedPanelTitle(result) }}</strong>
                    <p>{{ localizedPanelInsight(result) }}</p>
                    <div v-if="localizedRecommendations(result).length" class="ai-recommendations">
                      <b v-for="item in localizedRecommendations(result)" :key="item">{{ item }}</b>
                    </div>
                    <div class="ai-panel-actions">
                      <button
                        type="button"
                        class="model-action"
                        :disabled="isPanelRefreshing(result.id)"
                        @click="refreshModelAiPanel(result.id)"
                      >
                        {{ isModelPanelRefreshing(result.id) ? uiText.generatingModelPanel : uiText.generateModelPanel }}
                      </button>
                      <button
                        type="button"
                        class="rule-action"
                        :disabled="isPanelRefreshing(result.id)"
                        @click="refreshRuleAiPanel(result.id)"
                      >
                        {{ isRulePanelRefreshing(result.id) ? uiText.refreshingRulePanel : uiText.refreshRulePanel }}
                      </button>
                    </div>
                  </div>
                </article>
              </div>
            </section>
          </div>
        </div>
      </Transition>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { visualizationApi } from '@/api'
import { useRunSelector } from '@/composables/useRunSelector'
import { metricText } from '@/utils/modelDisplay'
import {
  Aim,
  Connection,
  DataLine,
  Document,
  Headset,
  Histogram,
  Odometer,
  PictureFilled,
  ScaleToOriginal,
  VideoCameraFilled,
  ArrowRight,
} from '@element-plus/icons-vue'

type Point = { x: number; y: number }
type ModulePreview = 'curve' | 'image' | 'audio' | 'text' | 'histogram' | 'embedding' | 'pr' | 'hparams' | 'graph' | 'profiler'
type ModuleProfile = {
  stage: string
  preview: ModulePreview
  accent: string
  desc: string
  miniBars: number[]
  lines: string[]
  stats: Array<{ label: string; value: string }>
}
type BaseModule = { key: string; icon: any; desc: string }
type VizModule = BaseModule & ModuleProfile
type SignalCard = { label: string; value: string; color: string; points: number[] }
type ComponentNode = { stage: string; name: string; bars: number[] }
type ScalarRecord = { step?: number; value?: number; tag?: string; wallTime?: number }
type HistogramRecord = { step?: number; limitsJson?: string; countsJson?: string; totalCount?: number }
type EmbeddingRecord = { step?: number; valuesJson?: string; label?: string; classId?: number; sampleId?: number }
type PRCurveRecord = { step?: number; precisionJson?: string; recallJson?: string; thresholdsJson?: string; numThresholds?: number }
type HParamRecord = { step?: number; metricValuesJson?: string; stringValuesJson?: string }
type ProfilerRecord = { step?: number; profilerJson?: string }
type RunType = 'training' | 'upload'
type AnalysisMode = 'oneToMany' | 'manyToMany'
type ResultViewMode = 'flat' | 'model' | 'module'
type ViewSaveTarget = 'cloud' | 'local'
type ViewSaveKind = 'text' | 'image' | 'both'
type ViewSaveFormat = 'md' | 'word' | 'json' | 'svg'
type RunLike = {
  id: number
  name?: string
  type: RunType
  architecture?: string
  modelArchitecture?: string
  status?: string
}
type AnalysisModuleOption = {
  key: string
  label?: string
  description?: string
  officialModels?: string[]
}
type AnalysisResult = {
  id: number
  moduleKey: string
  runId: number
  runType: RunType
  runName: string
  modelName?: string
  status: string
  score?: number | null
  recordCount: number
  latestStep?: number | null
  summary: string
  metrics?: Record<string, any>
  aiPanels?: Array<{ id: number; title: string; status?: string; aiModelName?: string; insightText?: string; recommendations?: string[] }>
}
type AnalysisPanel = NonNullable<AnalysisResult['aiPanels']>[number]
type AnalysisBatch = {
  id: number
  title: string
  status: string
  targetCount: number
  moduleCount: number
  results: AnalysisResult[]
}
type ResultFilterOption = {
  key: string
  label: string
  count: number
}
type ModuleSnapshot = {
  values: number[]
  secondary?: number[]
  steps?: number[]
  bars?: number[]
  nodes?: Array<{ x: number; y: number; r?: number; label?: string; value?: number }>
  edges?: Array<{ from: number; to: number }>
  rows?: Array<{ label: string; value: string }>
  summary?: string
  hasData: boolean
}
type ModuleProbe = {
  kind: 'curve' | 'bar' | 'node'
  index: number
  label: string
  value: string
  x: number
  y: number
  svgX: number
  svgY: number
}
type MiniProbe = { key: string; index: number; label: string; value: string; x: number; y: number }
const { runs, selectedRunId, loading, fetchRuns, selectRun } = useRunSelector()
const { locale } = useI18n()
const router = useRouter()

const chart = {
  width: 640,
  height: 260,
  left: 42,
  right: 26,
  top: 24,
  bottom: 34,
}

const baseModules: BaseModule[] = [
  { key: 'scalars', icon: DataLine, desc: '' },
  { key: 'images', icon: PictureFilled, desc: '' },
  { key: 'audio', icon: Headset, desc: '' },
  { key: 'text', icon: Document, desc: '' },
  { key: 'histograms', icon: Histogram, desc: '' },
  { key: 'embeddings', icon: Aim, desc: '' },
  { key: 'prCurves', icon: ScaleToOriginal, desc: '' },
  { key: 'hparams', icon: Connection, desc: '' },
  { key: 'graphs', icon: VideoCameraFilled, desc: '' },
  { key: 'profiler', icon: Odometer, desc: '' },
]

const moduleProfiles: Record<string, ModuleProfile> = {
  scalars: {
    stage: 'Signal',
    preview: 'curve',
    accent: '#42e6a4',
    desc: '多训练标量指标对比',
    miniBars: [34, 48, 42, 66, 58, 78],
    lines: ['series.loss()', 'smooth.window(8)', 'axis.sync(epoch)'],
    stats: [{ label: 'series', value: '12' }, { label: 'refresh', value: 'live' }],
  },
  images: {
    stage: 'Media',
    preview: 'image',
    accent: '#67e8f9',
    desc: '训练样本与特征图查看',
    miniBars: [72, 48, 84, 38, 64, 52],
    lines: ['feature.map()', 'sample.grid(4)', 'channel.inspect()'],
    stats: [{ label: 'tiles', value: '64' }, { label: 'mode', value: 'grid' }],
  },
  audio: {
    stage: 'Wave',
    preview: 'audio',
    accent: '#f59e0b',
    desc: '音频样本回放',
    miniBars: [28, 72, 36, 88, 44, 78],
    lines: ['waveform.slice()', 'mel.render()', 'cursor.play()'],
    stats: [{ label: 'clips', value: '18' }, { label: 'latency', value: '9ms' }],
  },
  text: {
    stage: 'Log',
    preview: 'text',
    accent: '#a78bfa',
    desc: '文本日志浏览',
    miniBars: [44, 54, 49, 62, 71, 58],
    lines: ['token.diff()', 'log.tail()', 'prompt.trace()'],
    stats: [{ label: 'tokens', value: '8k' }, { label: 'drift', value: 'low' }],
  },
  histograms: {
    stage: 'Tensor',
    preview: 'histogram',
    accent: '#fb7185',
    desc: '张量分布随训练演化',
    miniBars: [20, 34, 62, 86, 64, 28],
    lines: ['weights.bucket()', 'grad.norm()', 'bins.animate()'],
    stats: [{ label: 'bins', value: '48' }, { label: 'tail', value: 'soft' }],
  },
  embeddings: {
    stage: 'Vector',
    preview: 'embedding',
    accent: '#22c55e',
    desc: 'PCA / t-SNE 高维嵌入投影',
    miniBars: [62, 38, 72, 54, 46, 86],
    lines: ['embed.project()', 'cluster.trace()', 'knn.highlight()'],
    stats: [{ label: 'points', value: '2.4k' }, { label: 'space', value: '2D' }],
  },
  prCurves: {
    stage: 'Eval',
    preview: 'pr',
    accent: '#38bdf8',
    desc: 'PR 与 ROC 曲线分析',
    miniBars: [74, 69, 64, 58, 52, 44],
    lines: ['threshold.scan()', 'auc.measure()', 'curve.compare()'],
    stats: [{ label: 'AUC', value: '.94' }, { label: 'AP', value: '.91' }],
  },
  hparams: {
    stage: 'Config',
    preview: 'hparams',
    accent: '#fbbf24',
    desc: '超参数对比面板',
    miniBars: [42, 84, 55, 76, 36, 68],
    lines: ['lr.grid()', 'batch.compare()', 'parallel.axis()'],
    stats: [{ label: 'runs', value: '32' }, { label: 'best', value: 'v7' }],
  },
  graphs: {
    stage: 'Graph',
    preview: 'graph',
    accent: '#60a5fa',
    desc: '模型结构可视化',
    miniBars: [58, 58, 74, 74, 46, 46],
    lines: ['node.layout()', 'edge.flow()', 'layer.expand()'],
    stats: [{ label: 'nodes', value: '126' }, { label: 'edges', value: '241' }],
  },
  profiler: {
    stage: 'Runtime',
    preview: 'profiler',
    accent: '#f97316',
    desc: '性能剖析与火焰图',
    miniBars: [88, 62, 72, 38, 56, 44],
    lines: ['kernel.trace()', 'flame.merge()', 'gpu.timeline()'],
    stats: [{ label: 'GPU', value: '79%' }, { label: 'step', value: '18ms' }],
  },
}

const modules = computed<VizModule[]>(() => baseModules.map((mod) => ({
  ...mod,
  ...moduleProfiles[mod.key],
  desc: uiText.value.moduleDescs[mod.key] || moduleProfiles[mod.key].desc,
  stage: uiText.value.moduleStages[mod.key] || moduleProfiles[mod.key].stage,
})))

const activeModuleKey = ref(baseModules[0].key)
const displayedModuleKey = ref(baseModules[0].key)
const detailModuleKey = ref(baseModules[0].key)
const transitioningModule = ref<VizModule | null>(null)
const isModuleSwitching = ref(false)
const overviewProbe = ref<null | {
  index: number
  step: number
  x: number
  lossY: number
  accuracyY: number
  loss: string
  accuracy: string
  tooltipX: number
}>(null)
const distributionProbe = ref<null | {
  index: number
  label: string
  value: string
  x: number
  y: number
  tooltipX: number
}>(null)
const signalProbe = ref<null | { label: string; index: number; value: string; x: number; y: number }>(null)
const componentProbe = ref<null | { name: string; index: number; value: number; x: number; y: number }>(null)
const modulePointer = ref({ x: 50, y: 50 })
const moduleProbe = ref<ModuleProbe | null>(null)
const detailProbe = ref<ModuleProbe | null>(null)
const miniProbe = ref<MiniProbe | null>(null)
const detailPointer = ref({ x: 50, y: 50 })
const detailPanelRef = ref<HTMLElement>()
let transitionTimer: number | undefined
let moduleSwitchTimer: number | undefined
let pollTimer: number | undefined

const SCALAR_TAGS = ['train/loss', 'train/accuracy', 'val/loss', 'val/accuracy', 'train/learning_rate'] as const
const MODULE_TAG_HINTS: Record<string, string[]> = {
  images: ['images', 'image', 'sample', 'feature'],
  audio: ['audio', 'wave', 'mel'],
  text: ['text', 'log', 'prompt'],
  histograms: ['weights', 'weight', 'histogram', 'grad', 'gradient'],
  embeddings: ['embedding', 'embeddings', 'latent', 'features'],
  prCurves: ['pr', 'precision', 'recall', 'roc'],
  profiler: ['profiler', 'trace', 'runtime', 'flame'],
}

const dataLoading = ref(false)
const dataError = ref('')
const availableTags = ref<string[]>([])
const analysisModules = ref<AnalysisModuleOption[]>([])
const analysisMode = ref<AnalysisMode>('oneToMany')
const selectedAnalysisRunKeys = ref<string[]>([])
const selectedAnalysisModules = ref<string[]>(['scalars', 'histograms', 'hparams'])
const resultViewMode = ref<ResultViewMode>('flat')
const viewSavePanelOpen = ref(false)
const viewSaveTarget = ref<ViewSaveTarget>('cloud')
const viewSaveKind = ref<ViewSaveKind>('text')
const viewSaveFormat = ref<ViewSaveFormat>('md')
const selectedResultModelKeys = ref<string[]>([])
const selectedResultModuleKeys = ref<string[]>([])
const selectedAnalysisResultIds = ref<number[]>([])
const resultPersistenceBusy = ref<'save-view' | 'save-selected' | 'save-cloud-view' | 'import-chat' | ''>('')
const analysisRunning = ref(false)
const refreshingPanelIds = ref<Array<{ id: number; mode: 'model' | 'rule' }>>([])
const activeAnalysisBatch = ref<AnalysisBatch | null>(null)
const scalarSeries = ref<Record<string, ScalarRecord[]>>({})
const modulePayloads = ref<Record<string, any[]>>({
  images: [],
  audio: [],
  text: [],
  histograms: [],
  embeddings: [],
  prCurves: [],
  hparams: [],
  graphs: [],
  profiler: [],
})
const lastUpdatedAt = ref<Date | null>(null)
const selectedRunType = computed<RunType>(() => {
  const run = runs.value.find((item) => item.id === selectedRunId.value) as { type?: RunType } | undefined
  return run?.type || 'training'
})
const activeArchitecture = computed(() => selectedJob.value?.architecture || selectedJob.value?.modelArchitecture || '')

const activeModule = computed(() => modules.value.find((mod) => mod.key === displayedModuleKey.value) || modules.value[0])
const detailModule = computed(() => modules.value.find((mod) => mod.key === detailModuleKey.value) || modules.value[0])

onMounted(() => {
  const target = sessionStorage.getItem('vizModuleKey')
  const mod = modules.value.find((item) => item.key === target)
  if (mod) {
    activeModuleKey.value = mod.key
    displayedModuleKey.value = mod.key
    detailModuleKey.value = mod.key
    sessionStorage.removeItem('vizModuleKey')
  }
  void fetchAnalysisModules()
  void fetchLatestAnalysisBatch()
})

onBeforeUnmount(() => {
  window.clearTimeout(transitionTimer)
  window.clearTimeout(moduleSwitchTimer)
  window.clearInterval(pollTimer)
})

watch(
  () => runs.value,
  (items) => {
    if (!selectedRunId.value && items.length) {
      selectRun(items[0].id)
    }
    if (!selectedAnalysisRunKeys.value.length && items.length) {
      selectedAnalysisRunKeys.value = items.slice(0, 3).map((run) => runKey(run as RunLike))
    }
  },
  { immediate: true },
)

watch(
  () => selectedRunId.value,
  (runId) => {
    window.clearInterval(pollTimer)
    clearOverviewProbe()
    clearDistributionProbe()
    clearSignalProbe()
    clearComponentProbe()
    clearModuleProbe()
    clearDetailProbe()
    if (!runId) {
      resetVisualizationData()
      return
    }
    void fetchVisualizationData(true)
    pollTimer = window.setInterval(() => {
      void fetchVisualizationData(false)
    }, 5000)
  },
  { immediate: true },
)

function resetVisualizationData() {
  availableTags.value = []
  scalarSeries.value = {}
  modulePayloads.value = {
    images: [],
    audio: [],
    text: [],
    histograms: [],
    embeddings: [],
    prCurves: [],
    hparams: [],
    graphs: [],
    profiler: [],
  }
  dataError.value = ''
  lastUpdatedAt.value = null
}

async function fetchVisualizationData(showLoading = false) {
  const runId = selectedRunId.value
  if (!runId) return
  const runType = selectedRunType.value
  if (showLoading) dataLoading.value = true
  try {
    const tags = await fetchTags(runId, runType)
    const nextScalars: Record<string, ScalarRecord[]> = {}
    const nextPayloads: Record<string, any[]> = {
      images: [],
      audio: [],
      text: [],
      histograms: [],
      embeddings: [],
      prCurves: [],
      hparams: [],
      graphs: [],
      profiler: [],
    }

    const scalarResults = await Promise.allSettled(
      SCALAR_TAGS.map(async (tag) => {
        const response = await visualizationApi.getTrainingScalars(runId, runType, tag)
        return [tag, normalizeRecords<ScalarRecord>(response.data.data)] as const
      }),
    )
    scalarResults.forEach((result) => {
      if (result.status === 'fulfilled') nextScalars[result.value[0]] = result.value[1]
    })

    const moduleFetches = [
      fetchTaggedPayload(runId, runType, tags, 'images', (tag) => visualizationApi.getTrainingImages(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'audio', (tag) => visualizationApi.getTrainingAudio(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'text', (tag) => visualizationApi.getTrainingText(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'histograms', (tag) => visualizationApi.getTrainingHistograms(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'embeddings', (tag) => visualizationApi.getTrainingEmbeddings(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'prCurves', (tag) => visualizationApi.getTrainingPRCurves(runId, runType, tag)),
      fetchTaggedPayload(runId, runType, tags, 'profiler', (tag) => visualizationApi.getTrainingProfiler(runId, runType, tag)),
    ]
    const moduleResults = await Promise.allSettled(moduleFetches)
    moduleResults.forEach((result) => {
      if (result.status === 'fulfilled') nextPayloads[result.value.key] = result.value.data
    })

    const hparamsResponse = await safeApiCall(() => visualizationApi.getTrainingHParams(runId, runType))
    nextPayloads.hparams = normalizeRecords(hparamsResponse?.data.data)

    if (selectedRunId.value !== runId) return
    availableTags.value = tags
    scalarSeries.value = nextScalars
    modulePayloads.value = nextPayloads
    lastUpdatedAt.value = new Date()
    dataError.value = ''
  } catch (error: any) {
    if (selectedRunId.value === runId) {
      dataError.value = error?.response?.data?.message || error?.message || '实时数据读取失败'
    }
  } finally {
    if (selectedRunId.value === runId) dataLoading.value = false
  }
}

async function fetchTags(runId: number, runType: RunType) {
  const response = await safeApiCall(() => visualizationApi.getTrainingTags(runId, runType))
  const tags = normalizeRecords<string>(response?.data.data)
  return tags.length ? tags : [...SCALAR_TAGS]
}

async function fetchTaggedPayload(
  runId: number,
  runType: RunType,
  tags: string[],
  key: string,
  fetcher: (tag: string) => Promise<any>,
) {
  const tag = pickTag(tags, key)
  if (!tag) return { key, data: [] }
  const response = await safeApiCall(() => fetcher(tag))
  return { key, data: normalizeRecords(response?.data.data) }
}

async function safeApiCall<T>(request: () => Promise<T>) {
  try {
    return await request()
  } catch {
    return null
  }
}

function normalizeRecords<T>(records: unknown): T[] {
  return Array.isArray(records) ? records as T[] : []
}

function pickTag(tags: string[], moduleKey: string) {
  const hints = MODULE_TAG_HINTS[moduleKey] || []
  const lowerTags = tags.map((tag) => tag.toLowerCase())
  const exact = hints
    .flatMap((hint) => [hint, `${hint}s`, `train/${hint}`, `val/${hint}`])
    .find((candidate) => lowerTags.includes(candidate.toLowerCase()))
  if (exact) return tags[lowerTags.indexOf(exact.toLowerCase())]
  return tags.find((tag) => hints.some((hint) => tag.toLowerCase().includes(hint))) || tags[0]
}

const matrixModuleOptions = computed<AnalysisModuleOption[]>(() => {
  if (analysisModules.value.length) return analysisModules.value
  return modules.value.map((mod) => ({
    key: mod.key,
    label: mod.stage,
    description: mod.desc,
    officialModels: moduleFallbackModels(mod.key),
  }))
})

const activeAnalysisModeMeta = computed(() => analysisMode.value === 'oneToMany'
  ? { title: uiText.value.oneToManyTitle, desc: uiText.value.oneToManyDesc }
  : { title: uiText.value.matrixTitle, desc: uiText.value.matrixDesc })
const currentRunKey = computed(() => {
  const run = runs.value.find((item) => item.id === selectedRunId.value) as RunLike | undefined
  return run ? runKey(run) : ''
})
const canRunMatrixAnalysis = computed(() => selectedAnalysisRunKeys.value.length > 0 && selectedAnalysisModules.value.length > 0)
const canRunOneToManyAnalysis = computed(() => Boolean(currentRunKey.value) && selectedAnalysisModules.value.length > 0)
const matrixSelectionSummary = computed(() => {
  const cells = selectedAnalysisRunKeys.value.length * selectedAnalysisModules.value.length
  return uiText.value.matrixSelection
    .replace('{runs}', String(selectedAnalysisRunKeys.value.length))
    .replace('{modules}', String(selectedAnalysisModules.value.length))
    .replace('{cells}', String(cells))
})
const oneToManySelectionSummary = computed(() => uiText.value.oneToManySelection
  .replace('{run}', activeRunName.value)
  .replace('{modules}', String(selectedAnalysisModules.value.length)))
const oneToManyLiveModuleCount = computed(() => selectedAnalysisModules.value
  .filter((key) => moduleSnapshot(key).hasData).length)
const activeRunRecords = computed(() => selectedAnalysisModules.value
  .map((key) => {
    const snapshot = moduleSnapshot(key)
    return snapshot.values.length || snapshot.nodes?.length || 0
  })
  .reduce((sum, count) => sum + count, 0))
const matrixCellCount = computed(() => selectedAnalysisRunKeys.value.length * selectedAnalysisModules.value.length)
const selectedModuleChips = computed(() => selectedAnalysisModules.value.map((key) => ({
  key,
  title: moduleTitle(key),
  stage: moduleByKey(key)?.stage || key,
  accent: moduleAccent(key),
  hasData: moduleSnapshot(key).hasData,
  records: moduleSnapshot(key).values.length || moduleSnapshot(key).nodes?.length || 0,
})))
const resultFilterOptionsByModel = computed<ResultFilterOption[]>(() => buildResultFilterOptions('model'))
const resultFilterOptionsByModule = computed<ResultFilterOption[]>(() => buildResultFilterOptions('module'))
const activeResultFilterOptions = computed(() => resultViewMode.value === 'model'
  ? resultFilterOptionsByModel.value
  : resultFilterOptionsByModule.value)
const filteredAnalysisResults = computed(() => {
  const results = activeAnalysisBatch.value?.results || []
  if (resultViewMode.value === 'model' && selectedResultModelKeys.value.length) {
    const selected = new Set(selectedResultModelKeys.value)
    return results.filter((result) => selected.has(resultModelKey(result)))
  }
  if (resultViewMode.value === 'module' && selectedResultModuleKeys.value.length) {
    const selected = new Set(selectedResultModuleKeys.value)
    return results.filter((result) => selected.has(resultModuleKey(result)))
  }
  return results
})
const selectedAnalysisResults = computed(() => {
  const selected = new Set(selectedAnalysisResultIds.value)
  return (activeAnalysisBatch.value?.results || []).filter((result) => selected.has(result.id))
})
const resultSelectionSummary = computed(() => uiText.value.resultSelectionSummary
  .replace('{selected}', String(selectedAnalysisResultIds.value.length))
  .replace('{visible}', String(filteredAnalysisResults.value.length)))
const groupedAnalysisResults = computed(() => {
  if (resultViewMode.value === 'flat') return []
  const groups = new Map<string, { key: string; label: string; results: AnalysisResult[] }>()
  filteredAnalysisResults.value.forEach((result) => {
    const key = resultViewMode.value === 'model' ? resultModelKey(result) : resultModuleKey(result)
    const label = resultViewMode.value === 'model' ? resultModelLabel(result) : moduleTitle(result.moduleKey)
    const existing = groups.get(key)
    if (existing) {
      existing.results.push(result)
      return
    }
    groups.set(key, { key, label, results: [result] })
  })
  return [...groups.values()]
})

function setAnalysisMode(mode: AnalysisMode) {
  analysisMode.value = mode
  if (mode === 'manyToMany' && currentRunKey.value && !selectedAnalysisRunKeys.value.includes(currentRunKey.value)) {
    selectedAnalysisRunKeys.value = [currentRunKey.value, ...selectedAnalysisRunKeys.value]
  }
}

function visibleResultIds() {
  return filteredAnalysisResults.value.map((result) => result.id)
}

function isAnalysisResultSelected(resultId: number) {
  return selectedAnalysisResultIds.value.includes(resultId)
}

function toggleAnalysisResultSelection(resultId: number) {
  selectedAnalysisResultIds.value = isAnalysisResultSelected(resultId)
    ? selectedAnalysisResultIds.value.filter((id) => id !== resultId)
    : [...selectedAnalysisResultIds.value, resultId]
}

function selectVisibleAnalysisResults() {
  selectedAnalysisResultIds.value = visibleResultIds()
}

function clearAnalysisResultSelection() {
  selectedAnalysisResultIds.value = []
}

function activeResultIdsForAction(preferSelected: boolean) {
  if (preferSelected && selectedAnalysisResultIds.value.length) return [...selectedAnalysisResultIds.value]
  return visibleResultIds()
}

function runKey(run: RunLike) {
  return `${run.type || 'training'}:${run.id}`
}

function parseRunKey(key: string) {
  const [type, rawId] = key.split(':')
  return {
    runType: type === 'upload' ? 'upload' as RunType : 'training' as RunType,
    runId: Number(rawId),
  }
}

function moduleByKey(key: string) {
  return modules.value.find((mod) => mod.key === key)
}

function moduleAccent(key: string) {
  return moduleByKey(key)?.accent || activeModule.value.accent || '#42e6a4'
}

function shortLabel(value: string, max = 12) {
  if (!value || value.length <= max) return value
  return `${value.slice(0, Math.max(max - 3, 1))}...`
}

function moduleTitle(key: string) {
  const catalog = analysisModules.value.find((mod) => mod.key === key)
  return catalog?.label || uiText.value.moduleFullNames[key] || moduleByKey(key)?.stage || key
}

function moduleDescription(key: string, backendDescription?: string) {
  return backendDescription || uiText.value.moduleDescs[key] || moduleByKey(key)?.desc || uiText.value.emptyModule
}

function officialModelsForModule(key: string) {
  const catalog = analysisModules.value.find((mod) => mod.key === key)
  return (catalog?.officialModels?.length ? catalog.officialModels : moduleFallbackModels(key)).slice(0, 3)
}

function smartSelectAnalyzableModules() {
  const liveKeys = modules.value
    .filter((mod) => moduleSnapshot(mod.key).hasData)
    .map((mod) => mod.key)
  const fallbackKeys = ['scalars', 'histograms', 'hparams'].filter((key) => modules.value.some((mod) => mod.key === key))
  selectedAnalysisModules.value = liveKeys.length ? liveKeys : fallbackKeys
}

function clearSelectedAnalysisModules() {
  selectedAnalysisModules.value = []
}

function isModuleSelectedForActiveMode(key: string) {
  return selectedAnalysisModules.value.includes(key)
}

function moduleSelectionBadge(key: string) {
  if (!isModuleSelectedForActiveMode(key)) return ''
  return analysisMode.value === 'oneToMany' ? uiText.value.oneToManyBadge : uiText.value.manyToManyBadge
}

function ensureModuleSelected(key: string) {
  if (!selectedAnalysisModules.value.includes(key)) {
    selectedAnalysisModules.value = [...selectedAnalysisModules.value, key]
  }
}

function toggleModuleSelected(key: string) {
  if (selectedAnalysisModules.value.includes(key)) {
    selectedAnalysisModules.value = selectedAnalysisModules.value.filter((item) => item !== key)
    return
  }
  selectedAnalysisModules.value = [...selectedAnalysisModules.value, key]
}

function resultModelKey(result: AnalysisResult) {
  return `${result.runType}:${result.runId}:${result.modelName || result.runName || 'unknown'}`
}

function resultModuleKey(result: AnalysisResult) {
  return result.moduleKey || 'unknown'
}

function resultModelLabel(result: AnalysisResult) {
  return result.modelName || result.runName || `${runTypeLabel(result.runType)} #${result.runId}`
}

function buildResultFilterOptions(groupBy: 'model' | 'module') {
  const groups = new Map<string, ResultFilterOption>()
  ;(activeAnalysisBatch.value?.results || []).forEach((result) => {
    const key = groupBy === 'model' ? resultModelKey(result) : resultModuleKey(result)
    const label = groupBy === 'model' ? resultModelLabel(result) : moduleTitle(result.moduleKey)
    const existing = groups.get(key)
    if (existing) {
      existing.count += 1
      return
    }
    groups.set(key, { key, label, count: 1 })
  })
  return [...groups.values()]
}

function activeResultSelection() {
  return resultViewMode.value === 'model' ? selectedResultModelKeys : selectedResultModuleKeys
}

function setResultViewMode(mode: ResultViewMode) {
  resultViewMode.value = mode
}

function isResultFilterSelected(key: string) {
  if (resultViewMode.value === 'flat') return false
  return activeResultSelection().value.includes(key)
}

function toggleResultFilter(key: string) {
  if (resultViewMode.value === 'flat') return
  const selection = activeResultSelection()
  selection.value = selection.value.includes(key)
    ? selection.value.filter((item) => item !== key)
    : [...selection.value, key]
}

function selectAllResultFilters() {
  if (resultViewMode.value === 'flat') {
    selectedResultModelKeys.value = resultFilterOptionsByModel.value.map((option) => option.key)
    selectedResultModuleKeys.value = resultFilterOptionsByModule.value.map((option) => option.key)
    return
  }
  activeResultSelection().value = activeResultFilterOptions.value.map((option) => option.key)
}

function clearResultFilters() {
  if (resultViewMode.value === 'model') {
    selectedResultModelKeys.value = []
    return
  }
  if (resultViewMode.value === 'module') {
    selectedResultModuleKeys.value = []
    return
  }
  selectedResultModelKeys.value = []
  selectedResultModuleKeys.value = []
}

function moduleFallbackModels(key: string) {
  const fallback: Record<string, string[]> = {
    scalars: ['ResNet-50', 'EfficientNet-B4', 'ViT-B/16'],
    images: ['ResNet-50', 'Swin-T', 'DeepLabV3-RN50'],
    audio: ['Whisper-Tiny', 'Wav2Vec2-Base', 'AST-Base'],
    text: ['BERT-Base', 'GPT-2', 'T5-Small'],
    histograms: ['ResNet-101', 'ConvNeXt-T', 'DenseNet-201'],
    embeddings: ['ViT-B/16', 'BERT-Base', 'CLIP-ViT-B/32'],
    prCurves: ['YOLOv8n', 'YOLOv8s', 'DeepLabV3-RN50'],
    hparams: ['MobileNetV3-L', 'EfficientNet-B4', 'ResNet-50'],
    graphs: ['ResNet-50', 'Swin-T', 'LLaMA-7B'],
    profiler: ['YOLOv8n', 'MobileNetV3-L', 'Whisper-Tiny'],
  }
  return fallback[key] || ['ResNet-50', 'BERT-Base']
}

function runTypeLabel(type?: RunType) {
  return type === 'upload' ? uiText.value.uploadRun : uiText.value.trainingJob
}

function statusLabel(status?: string) {
  const labels: Record<string, string> = {
    queued: uiText.value.statusQueued,
    running: uiText.value.statusRunning,
    completed: uiText.value.statusCompleted,
    failed: uiText.value.statusFailed,
    paused: uiText.value.statusPaused,
    stopped: uiText.value.statusStopped,
    ready: uiText.value.statusReady,
    no_data: uiText.value.statusNoData,
  }
  return labels[status || ''] || status || uiText.value.statusUnknown
}

function resultStatusLabel(status?: string) {
  return statusLabel(status)
}

function translateLegacySummary(summary: string, result?: AnalysisResult) {
  if (!locale.value.startsWith('zh') || !summary) return summary
  const moduleName = result ? moduleTitle(result.moduleKey) : uiText.value.visualModules
  const tableMap: Record<string, string> = {
    scalar_logs: '标量日志表',
    image_logs: '图像日志表',
    audio_logs: '音频日志表',
    text_logs: '文本日志表',
    histogram_data: '直方图数据表',
    embedding_data: '向量嵌入表',
    hparam_data: '超参数表',
    profiler_data: '性能剖析表',
    pr_curve_data: 'PR 曲线表',
    roc_curve_data: 'ROC 曲线表',
  }
  const direct: Record<string, string> = {
    'Current training job metrics are available from the job record.': '已从训练任务记录中读取当前指标，可作为临时训练状态判断。',
    'Scalar metrics analyzed from real training_steps records.': '已基于真实训练步记录分析损失、准确率和学习率变化。',
    'Distribution derived from real training loss records.': '已用真实训练损失记录生成分布统计。',
    'Hyperparameters read from the training job configuration.': '已从训练任务配置中读取超参数信息。',
    'Architecture metadata is available for this training job.': '已读取该训练任务的模型架构元数据。',
    'Scalar logs analyzed from uploaded run data.': '已从上传运行数据中分析标量日志。',
    'Image logs analyzed from uploaded run data.': '已从上传运行数据中分析图像日志。',
    'Audio logs analyzed from uploaded run data.': '已从上传运行数据中分析音频日志。',
    'Text logs analyzed from uploaded run data.': '已从上传运行数据中分析文本日志。',
    'Histogram logs analyzed from uploaded run data.': '已从上传运行数据中分析分布记录。',
    'Embedding records analyzed from uploaded run data.': '已从上传运行数据中分析向量嵌入记录。',
    'Hyperparameter records analyzed from uploaded run data.': '已从上传运行数据中分析超参数记录。',
    'Profiler records analyzed from uploaded run data.': '已从上传运行数据中分析性能剖析记录。',
    'Evaluation curve records analyzed from uploaded PR/ROC logs.': '已合并上传运行中的 PR/ROC 评估曲线记录。',
    'No PR or ROC curve records were found.': '没有找到 PR 或 ROC 曲线记录，暂时无法做阈值评估对比。',
    'No graph topology table is connected for uploaded runs yet.': '上传运行暂未接入模型拓扑表，当前无法生成结构图分析。',
    'Unsupported module.': '暂不支持该分析模块。',
  }
  if (direct[summary]) return direct[summary]
  if (summary.startsWith('No ') && summary.includes(' log has been recorded for this training job.')) {
    return `该训练任务还没有记录「${moduleName}」模块需要的日志。`
  }
  if (summary.startsWith('No records found in ')) {
    const table = summary.replace('No records found in ', '').replace('.', '')
    return `${tableMap[table] || table} 中还没有找到该运行的记录。`
  }
  if (summary.startsWith('No scalar steps have been recorded')) {
    return '该训练任务还没有写入标量步记录，也没有可用的当前损失或准确率。'
  }
  if (summary.startsWith('No loss values are available')) {
    return '没有可用于生成分布直方图的训练损失记录。'
  }
  return summary
}

function localizedResultSummary(result: AnalysisResult) {
  return translateLegacySummary(result.summary, result) || uiText.value.emptyModule
}

function localizedPanelTitle(result: AnalysisResult) {
  const panel = result.aiPanels?.[0] as AnalysisPanel | undefined
  if (!panel?.title) return uiText.value.aiPanelReserved
  if (!locale.value.startsWith('zh')) return panel.title
  if (panel.title.endsWith(' AI panel')) return `${moduleTitle(result.moduleKey)} AI 解读`
  return panel.title
}

function localizedPanelInsight(result: AnalysisResult) {
  const panel = result.aiPanels?.[0] as AnalysisPanel | undefined
  const text = panel?.insightText || uiText.value.aiPanelReservedDesc
  if (!locale.value.startsWith('zh')) return text
  if (text.startsWith('Analyzed ')) {
    return `已针对「${result.runName}」使用「${moduleTitle(result.moduleKey)}」模块分析 ${result.recordCount} 条真实记录。${localizedResultSummary(result)}`
  }
  if (text.startsWith('The ') && text.includes('module is prepared')) {
    return `「${moduleTitle(result.moduleKey)}」模块已经为「${result.runName}」预留独立 AI 面板，但当前缺少必要记录。${localizedResultSummary(result)}`
  }
  return translateLegacySummary(text, result)
}

function localizedRecommendations(result: AnalysisResult) {
  const panel = result.aiPanels?.[0] as AnalysisPanel | undefined
  const recommendations = panel?.recommendations || []
  if (!locale.value.startsWith('zh')) return recommendations.slice(0, 3)
  return recommendations.slice(0, 3).map((item) => {
    if (item.startsWith('Connect SDK logging')) return `先接入「${moduleTitle(result.moduleKey)}」日志，再做横向对比。`
    if (item.startsWith('Run the same module')) return '可以先选择另一个已有记录的模型/运行，确认展示链路是否正常。'
    if (item.startsWith('Keep this AI panel')) return '保留这个 AI 面板，它能持续暴露缺少埋点或缺少数据的问题。'
    if (item.startsWith('Compare validation loss')) return '把多个模型的验证损失放在一起比较。'
    if (item.startsWith('Watch loss delta')) return '同时看损失变化和最佳准确率。'
    if (item.startsWith('Use early stopping')) return '如果验证损失持续回升，建议启用早停或降低学习率。'
    if (item.startsWith('Sort operators')) return '优先按单步耗时排序，定位最慢算子或数据阶段。'
    if (item.startsWith('Compare GPU')) return '并排比较不同模型的 GPU 利用率。'
    if (item.startsWith('Check data loading')) return '优化算子前先检查数据加载和预处理。'
    if (item.startsWith('Compare this result')) return '把这个结果和其他已选模型放在同一矩阵里比较。'
    if (item.startsWith('Use the record count')) return '记录数本身就是数据质量信号。'
    if (item.startsWith('Open the detail panel')) return '进入详情面板确认趋势和样本后再调整训练策略。'
    return item
  })
}

function activePanel(result: AnalysisResult) {
  return result.aiPanels?.[0] as AnalysisPanel | undefined
}

function panelStatusClass(result: AnalysisResult) {
  const status = activePanel(result)?.status
  return status === 'ai_generated' ? 'is-ai' : 'is-rule'
}

function panelStatusLabel(result: AnalysisResult) {
  const status = activePanel(result)?.status
  if (status === 'ai_generated') return uiText.value.aiGenerated
  if (status === 'rule_fallback') return uiText.value.ruleFallback
  return status || uiText.value.aiPanelReserved
}

function panelModelName(result: AnalysisResult) {
  return activePanel(result)?.aiModelName || uiText.value.aiModelUnknown
}

function isPanelRefreshing(resultId: number) {
  return refreshingPanelIds.value.some((item) => item.id === resultId)
}

function isModelPanelRefreshing(resultId: number) {
  return refreshingPanelIds.value.some((item) => item.id === resultId && item.mode === 'model')
}

function isRulePanelRefreshing(resultId: number) {
  return refreshingPanelIds.value.some((item) => item.id === resultId && item.mode === 'rule')
}

async function fetchAnalysisModules() {
  try {
    const response = await visualizationApi.getAnalysisModules()
    if (response.data.code === 200) {
      analysisModules.value = normalizeRecords<AnalysisModuleOption>(response.data.data)
    }
  } catch {
    analysisModules.value = []
  }
}

async function fetchLatestAnalysisBatch() {
  try {
    const response = await visualizationApi.listAnalysisBatches(1)
    const latest = normalizeRecords<any>(response.data.data)[0]
    if (!latest?.id) return
    const detail = await visualizationApi.getAnalysisBatch(Number(latest.id))
    if (detail.data.code === 200) {
      activeAnalysisBatch.value = normalizeAnalysisBatch(detail.data.data)
    }
  } catch {
    activeAnalysisBatch.value = null
  }
}

async function runMatrixAnalysis() {
  if (!canRunMatrixAnalysis.value) return
  analysisRunning.value = true
  try {
    const targets = selectedAnalysisRunKeys.value.map(parseRunKey).filter((target) => Number.isFinite(target.runId))
    const response = await visualizationApi.createAnalysisBatch({
      title: `${uiText.value.matrixTitle} ${new Date().toLocaleString()}`,
      targets,
      modules: selectedAnalysisModules.value,
    })
    if (response.data.code === 200) {
      activeAnalysisBatch.value = normalizeAnalysisBatch(response.data.data)
    }
  } finally {
    analysisRunning.value = false
  }
}

async function runOneToManyAnalysis() {
  if (!canRunOneToManyAnalysis.value) return
  analysisRunning.value = true
  try {
    const target = parseRunKey(currentRunKey.value)
    const response = await visualizationApi.createAnalysisBatch({
      title: `${uiText.value.oneToManyTitle} · ${activeRunName.value} · ${new Date().toLocaleString()}`,
      targets: [target],
      modules: selectedAnalysisModules.value,
    })
    if (response.data.code === 200) {
      activeAnalysisBatch.value = normalizeAnalysisBatch(response.data.data)
    }
  } finally {
    analysisRunning.value = false
  }
}

async function refreshAiPanel(resultId: number) {
  return refreshModelAiPanel(resultId)
}

async function refreshModelAiPanel(resultId: number) {
  await refreshAnalysisPanel(resultId, 'model')
}

async function refreshRuleAiPanel(resultId: number) {
  await refreshAnalysisPanel(resultId, 'rule')
}

async function refreshAnalysisPanel(resultId: number, mode: 'model' | 'rule') {
  if (isPanelRefreshing(resultId)) return
  refreshingPanelIds.value = [...refreshingPanelIds.value, { id: resultId, mode }]
  try {
    const response = mode === 'model'
      ? await visualizationApi.regenerateModelAiPanel(resultId)
      : await visualizationApi.regenerateRuleAiPanel(resultId)
    if (activeAnalysisBatch.value?.id) {
      const detail = await visualizationApi.getAnalysisBatch(activeAnalysisBatch.value.id)
      if (detail.data.code === 200) activeAnalysisBatch.value = normalizeAnalysisBatch(detail.data.data)
    }
    const panel = response.data.data as AnalysisPanel | undefined
    ElMessage.success(panel?.status === 'ai_generated' ? uiText.value.aiPanelRefreshed : uiText.value.aiPanelRuleRefreshed)
  } catch (error: any) {
    const message = error?.response?.data?.message || error?.message || uiText.value.unknownError
    ElMessage.error(`${mode === 'model' ? uiText.value.aiPanelModelFailed : uiText.value.aiPanelRuleFailed}: ${message}`)
  } finally {
    refreshingPanelIds.value = refreshingPanelIds.value.filter((item) => item.id !== resultId)
  }
}

async function saveAnalysisResults(preferSelected: boolean) {
  if (!activeAnalysisBatch.value?.id || resultPersistenceBusy.value) return
  const resultIds = activeResultIdsForAction(preferSelected)
  if (!resultIds.length) {
    ElMessage.warning(uiText.value.noResultToPersist)
    return
  }
  resultPersistenceBusy.value = preferSelected ? 'save-selected' : 'save-view'
  try {
    const response = await visualizationApi.saveAnalysisResults({
      batchId: activeAnalysisBatch.value.id,
      resultIds,
      title: activeAnalysisBatch.value.title,
    })
    const savedCount = Number(response.data.data?.savedCount ?? resultIds.length)
    ElMessage.success(uiText.value.resultSavedMessage.replace('{count}', String(savedCount)))
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || uiText.value.unknownError)
  } finally {
    resultPersistenceBusy.value = ''
  }
}

async function importAnalysisResultsToChat() {
  if (!activeAnalysisBatch.value?.id || resultPersistenceBusy.value) return
  const resultIds = activeResultIdsForAction(true)
  if (!resultIds.length) {
    ElMessage.warning(uiText.value.noResultToPersist)
    return
  }
  resultPersistenceBusy.value = 'import-chat'
  try {
    const response = await visualizationApi.importAnalysisResultsToChat({
      batchId: activeAnalysisBatch.value.id,
      resultIds,
      message: uiText.value.importChatSeed,
    })
    const conversationId = Number(response.data.data?.conversationId)
    ElMessage.success(uiText.value.importedToChatMessage)
    if (conversationId) {
      await router.push({ path: '/ai', query: { conversationId: String(conversationId) } })
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || uiText.value.unknownError)
  } finally {
    resultPersistenceBusy.value = ''
  }
}

function currentViewResults() {
  const selected = selectedAnalysisResults.value
  return selected.length ? selected : filteredAnalysisResults.value
}

function buildVisualViewMarkdown() {
  const batch = activeAnalysisBatch.value
  const results = currentViewResults()
  const lines = [
    `# ${batch?.title || 'DeepInsight 可视化分析视图'}`,
    '',
    `- 视图模式：${resultViewMode.value}`,
    `- 结果数量：${results.length}`,
    `- 导出时间：${new Date().toLocaleString('zh-CN')}`,
    '',
    '## 分析结果',
  ]
  results.forEach((result, index) => {
    lines.push(
      '',
      `### ${index + 1}. ${result.modelName || result.runName} / ${moduleTitle(result.moduleKey)}`,
      '',
      `- 状态：${resultStatusLabel(result.status)}`,
      `- 评分：${formatResultScore(result.score)}`,
      `- 记录数：${result.recordCount}`,
      `- 最新 step：${result.latestStep ?? '-'}`,
      '',
      localizedResultSummary(result),
    )
    const panel = activePanel(result)
    if (panel) {
      lines.push('', `**AI 面板：${localizedPanelTitle(result)}**`, '', localizedPanelInsight(result))
      const recommendations = localizedRecommendations(result)
      if (recommendations.length) {
        lines.push('', '建议：')
        recommendations.forEach((item) => lines.push(`- ${item}`))
      }
    }
  })
  return lines.join('\n')
}

function downloadBlob(content: BlobPart, filename: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[char] || char))
}

function buildVisualViewJson() {
  const batch = activeAnalysisBatch.value
  return JSON.stringify({
    title: batch?.title || 'DeepInsight 可视化分析视图',
    viewMode: resultViewMode.value,
    selectedResultIds: selectedAnalysisResultIds.value,
    exportedAt: new Date().toISOString(),
    results: currentViewResults(),
  }, null, 2)
}

function buildVisualViewWordHtml(content: string) {
  return `<!doctype html><html><head><meta charset="utf-8"><title>DeepInsight 可视化分析</title></head><body>${content
    .split('\n')
    .map((line) => line.startsWith('# ') ? `<h1>${escapeHtml(line.slice(2))}</h1>` : line.startsWith('## ') ? `<h2>${escapeHtml(line.slice(3))}</h2>` : line.startsWith('### ') ? `<h3>${escapeHtml(line.slice(4))}</h3>` : line.startsWith('- ') ? `<p>• ${escapeHtml(line.slice(2))}</p>` : line ? `<p>${escapeHtml(line)}</p>` : '<br>')
    .join('')}</body></html>`
}

function getCurrentVisualSvg() {
  const svg = document.querySelector<SVGElement>('.vizhub-page .overview-svg, .vizhub-page .detail-svg, .vizhub-page .distribution-svg')
  if (!svg) return ''
  const clone = svg.cloneNode(true) as SVGElement
  clone.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  return new XMLSerializer().serializeToString(clone)
}

function exportVisualView(format: 'md' | 'word' | 'json') {
  const content = buildVisualViewMarkdown()
  const stamp = Date.now()
  if (format === 'md') {
    downloadBlob(content, `deepinsight-visual-${stamp}.md`, 'text/markdown;charset=utf-8')
    return
  }
  if (format === 'json') {
    downloadBlob(buildVisualViewJson(), `deepinsight-visual-${stamp}.json`, 'application/json;charset=utf-8')
    return
  }
  downloadBlob(buildVisualViewWordHtml(content), `deepinsight-visual-${stamp}.doc`, 'application/msword;charset=utf-8')
}

function exportVisualImage() {
  const serialized = getCurrentVisualSvg()
  if (!serialized) {
    downloadBlob(buildVisualViewMarkdown(), `deepinsight-visual-snapshot-${Date.now()}.txt`, 'text/plain;charset=utf-8')
    ElMessage.info('当前视图没有可直接导出的 SVG，已导出文本快照')
    return
  }
  downloadBlob(serialized, `deepinsight-visual-${Date.now()}.svg`, 'image/svg+xml;charset=utf-8')
}

async function saveVisualViewToCloud(format: ViewSaveFormat = 'md', includeImage = false) {
  if (!activeAnalysisBatch.value?.id || resultPersistenceBusy.value) return
  resultPersistenceBusy.value = 'save-cloud-view'
  try {
    const markdown = buildVisualViewMarkdown()
    const svg = includeImage ? getCurrentVisualSvg() : ''
    const content = format === 'json'
      ? buildVisualViewJson()
      : format === 'word'
        ? buildVisualViewWordHtml(markdown)
        : format === 'svg'
          ? svg || markdown
          : markdown
    const response = await visualizationApi.saveVisualView({
      title: `${activeAnalysisBatch.value.title} / ${resultViewMode.value} / ${format}`,
      itemType: 'visual_view',
      sourceType: 'visual_analysis',
      format,
      content,
      imageDataUrl: svg,
      payload: {
        batchId: activeAnalysisBatch.value.id,
        viewMode: resultViewMode.value,
        saveKind: viewSaveKind.value,
        selectedResultIds: selectedAnalysisResultIds.value,
        resultIds: currentViewResults().map((result) => result.id),
      },
    })
    ElMessage.success(response.data.message || uiText.value.viewSavedCloud)
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '保存视图失败')
  } finally {
    resultPersistenceBusy.value = ''
  }
}

async function saveVisualViewWithOptions() {
  if (viewSaveTarget.value === 'cloud') {
    await saveVisualViewToCloud(viewSaveFormat.value, viewSaveKind.value !== 'text')
    viewSavePanelOpen.value = false
    return
  }

  if (viewSaveKind.value === 'image' || viewSaveFormat.value === 'svg') {
    exportVisualImage()
  } else if (viewSaveFormat.value === 'json') {
    exportVisualView('json')
  } else if (viewSaveFormat.value === 'word') {
    exportVisualView('word')
  } else {
    exportVisualView('md')
  }

  if (viewSaveKind.value === 'both' && viewSaveFormat.value !== 'svg') {
    exportVisualImage()
  }
  viewSavePanelOpen.value = false
}

function normalizeAnalysisBatch(batch: any): AnalysisBatch {
  const normalized = {
    id: Number(batch?.id ?? 0),
    title: String(batch?.title || uiText.value.matrixTitle),
    status: String(batch?.status || 'completed'),
    targetCount: Number(batch?.targetCount ?? 0),
    moduleCount: Number(batch?.moduleCount ?? 0),
    results: normalizeRecords<any>(batch?.results).map((result) => ({
      id: Number(result.id),
      moduleKey: String(result.moduleKey),
      runId: Number(result.runId),
      runType: result.runType === 'upload' ? 'upload' : 'training',
      runName: String(result.runName || ''),
      modelName: result.modelName ? String(result.modelName) : '',
      status: String(result.status || 'no_data'),
      score: result.score === null || result.score === undefined ? null : Number(result.score),
      recordCount: Number(result.recordCount ?? 0),
      latestStep: result.latestStep === null || result.latestStep === undefined ? null : Number(result.latestStep),
      summary: String(result.summary || ''),
      metrics: result.metrics || {},
      aiPanels: normalizeRecords<any>(result.aiPanels),
    })),
  }
  const validIds = new Set(normalized.results.map((result) => result.id))
  selectedAnalysisResultIds.value = selectedAnalysisResultIds.value.filter((id) => validIds.has(id))
  return normalized
}

function formatResultScore(score?: number | null) {
  if (score === null || score === undefined || !Number.isFinite(score)) return '--'
  return score >= 1 ? score.toFixed(2) : `${(score * 100).toFixed(1)}%`
}

const uiText = computed(() => {
  const isZh = locale.value.startsWith('zh')
  return {
    visualizationLayer: isZh ? '可视化层' : 'Visualization Layer',
    headerDesc: isZh
      ? '选择训练运行，查看当前模型已记录的标量、媒体、结构与性能数据。没有日志的模块会显示缺失原因和后续接入建议。'
      : 'Select a run to inspect real scalar, media, graph, and profiler logs for the current model. Modules without logs show a clear empty state.',
    modules: isZh ? '模块' : 'modules',
    trainingRun: isZh ? '训练运行' : 'Run',
    selectRun: isZh ? '选择训练运行' : 'Select run',
    refresh: isZh ? '刷新' : 'Refresh',
    overviewPreview: isZh ? '总览预览' : 'Overview Preview',
    trainingSignal: isZh ? '训练信号' : 'Training Signal',
    lossAccuracy: isZh ? '损失 / 准确率' : 'Loss / Accuracy',
    lossShort: metricText('loss', locale).shortLabel,
    accuracyShort: metricText('accuracy', locale).shortLabel,
    lossLegend: metricText('loss', locale).label,
    accuracyLegend: metricText('accuracy', locale).label,
    lossAccuracyExplain: isZh
      ? 'Loss 是模型误差，通常越低越好；Accuracy 是预测正确率，通常越高越好。这里读取当前 run 的真实标量日志。'
      : 'Loss is prediction error, usually lower is better. Accuracy is correctness, usually higher is better. This chart reads real scalar logs from the current run.',
    distribution: isZh ? '分布' : 'Distribution',
    weights: isZh ? '权重/梯度' : 'Weights / Gradients',
    bin: isZh ? '区间' : 'bin',
    weight: isZh ? '权重' : 'weight',
    step: isZh ? '步' : 'step',
    liveComponents: isZh ? '实时组件' : 'Live Components',
    signalFlow: isZh ? '核心信号与组件流' : 'Core Signals and Component Flow',
    moduleSwitcher: isZh ? '模块切换器' : 'Module Switcher',
    visualModules: isZh ? '可视化分析模块' : 'Visualization Modules',
    analysisModeEyebrow: isZh ? '分析模式' : 'Analysis Mode',
    analysisModeSwitch: isZh ? '切换分析模式' : 'Switch analysis mode',
    oneToMany: isZh ? '一对多' : 'One-to-many',
    manyToMany: isZh ? '多对多' : 'Many-to-many',
    oneToManyShort: isZh ? '单模型多模块' : 'single run',
    manyToManyShort: isZh ? '多模型矩阵' : 'matrix',
    oneToManyEyebrow: isZh ? '一对多执行' : 'One-to-many execution',
    oneToManyTitle: isZh ? '当前模型 × 多分析' : 'Current Model x Multiple Analyses',
    oneToManyDesc: isZh
      ? '锁定顶部当前训练任务或上传运行，然后选择多个分析模块。执行后会为这个模型的每个模块生成真实落库结果和独立 AI 面板。'
      : 'Lock the current run selected above, choose multiple analysis modules, then create one persisted result and AI panel for every module.',
    oneToManySelection: isZh ? '当前「{run}」× {modules} 个模块' : 'Current "{run}" x {modules} modules',
    oneToManyBadge: isZh ? '一对多已选' : 'one-to-many',
    manyToManyBadge: isZh ? '多对多已选' : 'many-to-many',
    clickToAdd: isZh ? '点击加入当前模式' : 'click to add',
    focusModel: isZh ? '当前模型' : 'Focus model',
    focusModelDesc: isZh
      ? '这个模式会自动使用顶部已选运行，不再让你重复选择模型；下面模块卡片也会同步标记已加入分析。'
      : 'This mode automatically uses the run selected above; module cards below stay marked as included.',
    selectedModules: isZh ? '已选模块' : 'selected modules',
    noModulePicked: isZh ? '还没有选择模块，请点模块卡片右下角的选择按钮加入分析。' : 'No modules selected yet. Use the bottom-right button on each module card to include it.',
    smartSelectModules: isZh ? '智能选择可分析模块' : 'Smart select analyzable',
    clearSelectedModules: isZh ? '清空选择' : 'Clear selection',
    selectedForAnalysis: isZh ? '已加入分析' : 'Included',
    selectForAnalysis: isZh ? '选择分析' : 'Analyze',
    canAnalyze: isZh ? '可分析' : 'ready',
    noDataShort: isZh ? '缺数据' : 'no logs',
    liveModules: isZh ? '有数据模块' : 'live modules',
    runOneToMany: isZh ? '执行一对多分析' : 'Run one-to-many',
    matrixEyebrow: isZh ? '多对多执行' : 'Many-to-many execution',
    matrixTitle: isZh ? '模型 × 分析矩阵' : 'Model x Analysis Matrix',
    matrixDesc: isZh
      ? '左边选择多个训练任务或上传运行，右边选择多个分析模块。点击执行后，系统会为每个「模型 × 模块」组合生成一条真实落库结果，并为每条结果保留独立 AI 分析面板。'
      : 'Select multiple training jobs or uploaded runs on the left, then choose analysis modules on the right. Running the matrix creates one persisted result and one dedicated AI panel for every model x module cell.',
    matrixSelection: isZh ? '已选择 {runs} 个运行 × {modules} 个模块 = {cells} 个结果单元' : '{runs} runs x {modules} modules = {cells} result cells',
    matrixModels: isZh ? '选择模型 / 运行' : 'Select models / runs',
    matrixModules: isZh ? '选择分析模块' : 'Select analysis modules',
    runMatrix: isZh ? '执行矩阵分析' : 'Run matrix analysis',
    matrixResults: isZh ? '落库结果' : 'Persisted results',
    matrixCells: isZh ? '个结果单元' : 'result cells',
    resultFlat: isZh ? '全部平铺' : 'Flat',
    resultByModel: isZh ? '按模型' : 'By model',
    resultByModule: isZh ? '按模块' : 'By module',
    resultSelectAll: isZh ? '全选当前分组' : 'Select all',
    resultClearFilters: isZh ? '显示全部' : 'Show all',
    aiPanel: isZh ? 'AI 分析面板' : 'AI analysis panel',
    aiPanelReserved: isZh ? '专属面板已预留' : 'Dedicated panel reserved',
    aiPanelReservedDesc: isZh ? '该模块的 AI 分析会跟随结果单独保存和刷新。' : 'This module keeps its own saved and refreshable AI analysis.',
    refreshAiPanel: isZh ? '刷新 AI 面板' : 'Refresh AI panel',
    refreshingAiPanel: isZh ? '刷新中...' : 'Refreshing...',
    generateModelPanel: isZh ? '调用 AI 模型' : 'Run AI model',
    generatingModelPanel: isZh ? '模型生成中...' : 'Generating...',
    refreshRulePanel: isZh ? '刷新规则分析' : 'Refresh rules',
    refreshingRulePanel: isZh ? '规则刷新中...' : 'Refreshing rules...',
    aiGenerated: isZh ? '模型生成' : 'AI generated',
    ruleFallback: isZh ? '规则兜底' : 'Rule fallback',
    aiModelUnknown: isZh ? '未标记模型' : 'Unknown model',
    aiPanelRefreshed: isZh ? 'AI 面板已调用模型刷新' : 'AI panel refreshed with model output',
    aiPanelRuleRefreshed: isZh ? '规则分析已刷新' : 'Rule analysis refreshed',
    aiPanelFallbackRefreshed: isZh ? 'AI 模型不可用，已刷新规则分析兜底' : 'AI unavailable, refreshed rule fallback',
    aiPanelRefreshFailed: isZh ? 'AI 面板刷新失败' : 'AI panel refresh failed',
    aiPanelModelFailed: isZh ? 'AI 模型生成失败' : 'AI model generation failed',
    aiPanelRuleFailed: isZh ? '规则分析刷新失败' : 'Rule analysis refresh failed',
    unknownError: isZh ? '未知错误' : 'unknown error',
    detail: isZh ? '详情' : 'Detail',
    pointer: isZh ? '鼠标探针' : 'Pointer',
    records: isZh ? '记录数' : 'records',
    status: isZh ? '状态' : 'status',
    latestStep: isZh ? '最新步' : 'latest step',
    uploadRun: isZh ? '上传运行' : 'uploaded run',
    trainingJob: isZh ? '训练任务' : 'training job',
    statusQueued: isZh ? '排队中' : 'queued',
    statusRunning: isZh ? '运行中' : 'running',
    statusCompleted: isZh ? '已完成' : 'completed',
    statusFailed: isZh ? '失败' : 'failed',
    statusPaused: isZh ? '已暂停' : 'paused',
    statusStopped: isZh ? '已停止' : 'stopped',
    statusReady: isZh ? '已生成' : 'ready',
    statusNoData: isZh ? '缺少日志' : 'missing logs',
    statusUnknown: isZh ? '未知状态' : 'unknown',
    selected: isZh ? '选中项' : 'selected',
    peak: isZh ? '峰值' : 'peak',
    low: isZh ? '低值' : 'low',
    mean: isZh ? '均值' : 'mean',
    architecture: isZh ? '模型架构' : 'architecture',
    realRecords: isZh ? '条真实记录' : 'real records',
    steps: isZh ? '步记录' : 'steps',
    bins: isZh ? '个直方图区间' : 'bins',
    total: isZh ? '总计' : 'total',
    classes: isZh ? '类别数' : 'classes',
    thresholds: isZh ? '阈值数' : 'thresholds',
    maxPrecision: isZh ? '最高精确率' : 'max precision',
    hyperMetricCount: isZh ? '个超参数指标' : 'hyperparameter metrics',
    profilerSamples: isZh ? '个性能样本' : 'profiler samples',
    projectedVectors: isZh ? '个投影向量' : 'projected vectors',
    moduleStages: {
      scalars: isZh ? '标量' : 'Signal',
      images: isZh ? '图像' : 'Media',
      audio: isZh ? '音频' : 'Wave',
      text: isZh ? '文本' : 'Log',
      histograms: isZh ? '张量' : 'Tensor',
      embeddings: isZh ? '向量' : 'Vector',
      prCurves: isZh ? '评估' : 'Eval',
      hparams: isZh ? '配置' : 'Config',
      graphs: isZh ? '结构' : 'Graph',
      profiler: isZh ? '运行时' : 'Runtime',
    } as Record<string, string>,
    moduleFullNames: {
      scalars: isZh ? '标量趋势' : 'Scalars',
      images: isZh ? '图像样本' : 'Images',
      audio: isZh ? '音频波形' : 'Audio',
      text: isZh ? '文本日志' : 'Text',
      histograms: isZh ? '分布直方图' : 'Histograms',
      embeddings: isZh ? '向量投影' : 'Embeddings',
      prCurves: isZh ? 'PR/ROC 评估' : 'PR / ROC',
      hparams: isZh ? '超参数对比' : 'Hyperparameters',
      graphs: isZh ? '模型结构' : 'Graphs',
      profiler: isZh ? '性能剖析' : 'Profiler',
    } as Record<string, string>,
    moduleDescs: {
      scalars: isZh ? '损失、准确率、学习率等训练标量，用来判断是否收敛。' : 'Training scalars such as loss, accuracy, and learning rate.',
      images: isZh ? '训练样本、特征图或中间可视结果，来自当前 run 的图像日志。' : 'Image logs such as samples, feature maps, or intermediate views.',
      audio: isZh ? '音频波形、梅尔谱等记录，用来检查音频模型输入输出。' : 'Audio logs such as waveforms and mel spectrograms.',
      text: isZh ? '文本日志、样本预测或提示词轨迹，用来追踪语言模型行为。' : 'Text logs, predictions, or prompt traces.',
      histograms: isZh ? metricText('histogram', locale).description : metricText('histogram', locale).description,
      embeddings: isZh ? metricText('embedding', locale).description : metricText('embedding', locale).description,
      prCurves: isZh ? metricText('prCurve', locale).description : metricText('prCurve', locale).description,
      hparams: isZh ? '学习率、批次大小、正则化等超参数与指标的对应关系。' : 'Relationship between hyperparameters and metrics.',
      graphs: isZh ? '模型层、节点和连接关系。若未上传拓扑日志，只显示当前架构名。' : 'Model layers, nodes, and edges. Without topology logs, only the architecture is shown.',
      profiler: isZh ? metricText('profiler', locale).description : metricText('profiler', locale).description,
    } as Record<string, string>,
    noRun: isZh ? '请选择一个训练任务或上传运行。' : 'Select a training job or uploaded run.',
    loading: isZh ? '正在读取当前模型实时数据...' : 'Loading live data for the current model...',
    error: isZh ? '数据读取失败' : 'Unable to load data',
    emptyScalars: isZh ? '当前 run 暂无 Loss / Accuracy 标量记录。请在训练代码中记录 train/loss、train/accuracy 等标量。' : 'No Loss / Accuracy scalar records for this run. Log train/loss and train/accuracy from training code.',
    emptyModule: isZh ? '当前 run 暂无该模块真实日志。' : 'No real logs for this module in the current run.',
    source: isZh ? '数据源' : 'Source',
    updated: isZh ? '更新' : 'Updated',
    live: isZh ? '实时' : 'live',
    missing: isZh ? '暂无数据' : 'No data',
    resultSelectionSummary: isZh ? '已选 {selected} 条 / 当前视图 {visible} 条' : '{selected} selected / {visible} visible',
    selectVisibleResults: isZh ? '选中当前视图' : 'Select visible',
    clearResultSelection: isZh ? '清空选中' : 'Clear selected',
    saveVisibleResults: isZh ? '一键保存当前视图' : 'Save visible',
    saveSelectedResults: isZh ? '保存选中' : 'Save selected',
    saveViewCloud: isZh ? '视图存云端' : 'Save view cloud',
    saveViewPanel: isZh ? '保存视图/导出' : 'Save / Export view',
    saveViewPanelDesc: isZh ? '保存当前筛选或选中的矩阵结果，可写入云端数据库，也可导出本地文字或图像文件。' : 'Save the current filtered or selected matrix view to cloud storage, or export text/image files locally.',
    saveTarget: isZh ? '保存位置' : 'Target',
    saveTargetCloud: isZh ? '云端数据库' : 'Cloud database',
    saveTargetLocal: isZh ? '本地文件' : 'Local file',
    saveContent: isZh ? '保存内容' : 'Content',
    saveContentText: isZh ? '文字记录' : 'Text record',
    saveContentImage: isZh ? '图像快照' : 'Image snapshot',
    saveContentBoth: isZh ? '文字 + 图像' : 'Text + image',
    saveFormat: isZh ? '文件格式' : 'Format',
    executeSave: isZh ? '执行保存' : 'Save now',
    close: isZh ? '关闭' : 'Close',
    exportMd: isZh ? '导出 MD' : 'Export MD',
    exportWord: isZh ? '导出 Word' : 'Export Word',
    exportImage: isZh ? '导出图片' : 'Export image',
    savingResults: isZh ? '保存中...' : 'Saving...',
    importToAiChat: isZh ? '导入 AI 对话' : 'Import to AI chat',
    importingToChat: isZh ? '导入中...' : 'Importing...',
    resultSelect: isZh ? '选择' : 'Select',
    resultSelected: isZh ? '已选中' : 'Selected',
    noResultToPersist: isZh ? '当前没有可保存或导入的分析结果' : 'No analysis results to save or import',
    resultSavedMessage: isZh ? '已保存 {count} 条分析记录到 MySQL' : 'Saved {count} analysis records to MySQL',
    importedToChatMessage: isZh ? '已保存并生成新的 AI 对话记录' : 'Saved and created a new AI conversation',
    viewSavedCloud: isZh ? '视图已保存到云端' : 'View saved to cloud',
    importChatSeed: isZh
      ? '请基于这些已保存的训练/可视化分析记录继续分析，给出风险、证据、下一步实验和优先级。'
      : 'Continue analyzing these saved training/visualization records. Give risks, evidence, next experiments, and priorities.',
    graphMissing: isZh
      ? '当前模型为 {model}，但这个 run 还没有上传模型结构拓扑日志。'
      : 'Current model: {model}. No graph topology log has been uploaded for this run.',
  }
})

const dataStatusLabel = computed(() => {
  if (!selectedRunId.value) return uiText.value.noRun
  if (dataLoading.value) return uiText.value.loading
  if (dataError.value) return `${uiText.value.error}: ${dataError.value}`
  if (!lastUpdatedAt.value) return uiText.value.missing
  return `${uiText.value.updated} ${lastUpdatedAt.value.toLocaleTimeString()}`
})

function displayRunLabel(run: { name?: string; architecture?: string; modelArchitecture?: string; status?: string }) {
  const arch = run.architecture || run.modelArchitecture
  return arch ? `${run.name} · ${arch}` : run.name || uiText.value.missing
}

const hasScalarData = computed(() => curveData.value.loss.length > 0 || curveData.value.accuracy.length > 0)
const selectedJob = computed(() => runs.value.find((run) => run.id === selectedRunId.value) as any)

function setActiveModule(key: string) {
  if (transitioningModule.value || key === activeModuleKey.value) return
  activeModuleKey.value = key
  window.clearTimeout(moduleSwitchTimer)
  isModuleSwitching.value = true
  moduleSwitchTimer = window.setTimeout(() => {
    displayedModuleKey.value = key
    clearModuleProbe()
    window.setTimeout(() => {
      isModuleSwitching.value = false
    }, 120)
  }, 90)
}

function openModule(mod: VizModule) {
  if (transitioningModule.value) return
  activeModuleKey.value = mod.key
  displayedModuleKey.value = mod.key
  detailModuleKey.value = mod.key
  detailProbe.value = null
  transitioningModule.value = mod
  window.clearTimeout(transitionTimer)
  transitionTimer = window.setTimeout(() => {
    transitioningModule.value = null
  }, 520)
  void nextTick(() => {
    detailPanelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

function localPoint(event: PointerEvent, width: number, height: number) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  return {
    x: clamp(((event.clientX - rect.left) / rect.width) * width, 0, width),
    y: clamp(((event.clientY - rect.top) / rect.height) * height, 0, height),
    px: clamp(((event.clientX - rect.left) / rect.width) * 100, 4, 96),
    py: clamp(((event.clientY - rect.top) / rect.height) * 100, 6, 92),
  }
}

function closestIndexByX(points: Point[], x: number) {
  if (!points.length) return 0
  return points.reduce((closest, point, index) => Math.abs(point.x - x) < Math.abs(points[closest].x - x) ? index : closest, 0)
}

function updatePointerTarget(target: { value: { x: number; y: number } }, x: number, y: number, threshold = 0.9) {
  const current = target.value
  if (Math.abs(current.x - x) < threshold && Math.abs(current.y - y) < threshold) return
  target.value = { x, y }
}

function sameModuleProbe(current: ModuleProbe | null, next: ModuleProbe) {
  return !!current &&
    current.kind === next.kind &&
    current.index === next.index &&
    current.label === next.label &&
    current.value === next.value
}

function sameMiniProbe(current: MiniProbe | null, next: MiniProbe) {
  return !!current &&
    current.key === next.key &&
    current.index === next.index &&
    current.label === next.label &&
    current.value === next.value
}

function handleOverviewPointer(event: PointerEvent) {
  if (!lossPoints.value.length && !accuracyPoints.value.length) return
  const point = localPoint(event, chart.width, chart.height)
  const primaryPoints = lossPoints.value.length ? lossPoints.value : accuracyPoints.value
  const index = closestIndexByX(primaryPoints, point.x)
  const lossPoint = lossPoints.value[index] || primaryPoints[index] || lastLossPoint.value
  const accuracyPoint = accuracyPoints.value[index] || primaryPoints[index] || lastAccuracyPoint.value
  const nextProbe = {
    index,
    step: curveData.value.steps[index] || index + 1,
    x: lossPoint.x,
    lossY: lossPoint.y,
    accuracyY: accuracyPoint.y,
    loss: (curveData.value.loss[index] || 0).toFixed(3),
    accuracy: (curveData.value.accuracy[index] || 0).toFixed(1),
    tooltipX: clamp(lossPoint.x + 10, 48, 492),
  }
  if (
    overviewProbe.value &&
    overviewProbe.value.index === nextProbe.index &&
    overviewProbe.value.loss === nextProbe.loss &&
    overviewProbe.value.accuracy === nextProbe.accuracy
  ) return
  overviewProbe.value = nextProbe
}

function clearOverviewProbe() {
  overviewProbe.value = null
}

function handleDistributionPointer(event: PointerEvent) {
  const point = localPoint(event, 320, 160)
  const bars = distributionBars.value
  if (!bars.length) return
  const index = bars.reduce((closest, bar, barIndex) => Math.abs(bar.centerX - point.x) < Math.abs(bars[closest].centerX - point.x) ? barIndex : closest, 0)
  const bar = bars[index]
  const nextProbe = {
    index,
    label: bar.label,
    value: bar.value.toFixed(1),
    x: bar.centerX,
    y: bar.centerY,
    tooltipX: clamp(bar.centerX + 8, 22, 194),
  }
  if (
    distributionProbe.value &&
    distributionProbe.value.index === nextProbe.index &&
    distributionProbe.value.value === nextProbe.value
  ) return
  distributionProbe.value = nextProbe
}

function clearDistributionProbe() {
  distributionProbe.value = null
}

function handleSignalPointer(event: PointerEvent, signal: SignalCard) {
  if (!signal.points.length) return
  const point = localPoint(event, 1, 1)
  const index = clamp(Math.round((point.px / 100) * (signal.points.length - 1)), 0, signal.points.length - 1)
  const nextProbe = {
    label: signal.label,
    index,
    value: `${signal.points[index]}%`,
    x: clamp((index / Math.max(signal.points.length - 1, 1)) * 100, 10, 90),
    y: 50,
  }
  if (
    signalProbe.value &&
    signalProbe.value.label === nextProbe.label &&
    signalProbe.value.index === nextProbe.index &&
    signalProbe.value.value === nextProbe.value
  ) return
  signalProbe.value = nextProbe
}

function clearSignalProbe() {
  signalProbe.value = null
}

function handleComponentPointer(event: PointerEvent, node: ComponentNode) {
  if (!node.bars.length) return
  const point = localPoint(event, 1, 1)
  const index = clamp(Math.round((point.px / 100) * (node.bars.length - 1)), 0, node.bars.length - 1)
  const nextProbe = {
    name: node.name,
    index,
    value: node.bars[index],
    x: clamp((index / Math.max(node.bars.length - 1, 1)) * 100, 12, 88),
    y: 54,
  }
  if (
    componentProbe.value &&
    componentProbe.value.name === nextProbe.name &&
    componentProbe.value.index === nextProbe.index &&
    componentProbe.value.value === nextProbe.value
  ) return
  componentProbe.value = nextProbe
}

function clearComponentProbe() {
  componentProbe.value = null
}

function moduleProbeLabel(mod: VizModule, index: number) {
  const labels: Record<string, string[]> = {
    scalars: locale.value.startsWith('zh') ? ['损失', '准确率', '学习率', '梯度', '验证损失', '验证准确率'] : ['loss', 'accuracy', 'lr', 'grad', 'val/loss', 'val/acc'],
    images: locale.value.startsWith('zh') ? ['输入图', '卷积层1', '模块2', '注意力图', '解码器', '样本'] : ['input', 'conv1', 'block2', 'attention', 'decoder', 'sample'],
    audio: locale.value.startsWith('zh') ? ['波形', '梅尔谱1', '梅尔谱2', '音高', '能量', '静音段'] : ['wave', 'mel-1', 'mel-2', 'pitch', 'energy', 'silence'],
    text: locale.value.startsWith('zh') ? ['词元', '对数概率', '提示词', '回答', '差异', '轨迹'] : ['token', 'logprob', 'prompt', 'answer', 'diff', 'trace'],
    histograms: locale.value.startsWith('zh') ? ['1分位', '10分位', '中位数', '90分位', '尾部', '异常值'] : ['p1', 'p10', 'median', 'p90', 'tail', 'outlier'],
    embeddings: locale.value.startsWith('zh') ? ['类别A', '类别B', '类别C', '近邻', '簇', '锚点'] : ['class A', 'class B', 'class C', 'neighbor', 'cluster', 'anchor'],
    prCurves: locale.value.startsWith('zh') ? ['阈值 .10', '阈值 .25', '阈值 .40', '阈值 .55', '阈值 .70', '阈值 .85'] : ['thr .10', 'thr .25', 'thr .40', 'thr .55', 'thr .70', 'thr .85'],
    hparams: locale.value.startsWith('zh') ? ['学习率', '批次', 'Dropout', '权重衰减', '随机种子', '得分'] : ['lr', 'batch', 'dropout', 'wd', 'seed', 'score'],
    graphs: locale.value.startsWith('zh') ? ['输入层', '卷积', '归一化', '注意力', '输出头', '输出层'] : ['input', 'conv', 'norm', 'attn', 'head', 'output'],
    profiler: locale.value.startsWith('zh') ? ['拷贝', '卷积', '归一化', '激活', '矩阵乘', 'softmax'] : ['copy', 'conv', 'norm', 'relu', 'matmul', 'softmax'],
  }
  const options = labels[mod.key] || [mod.stage]
  return options[index % options.length]
}

function formatProbeValue(mod: VizModule, raw: number, index: number) {
  if (mod.key === 'scalars') return `${moduleProbeLabel(mod, index)} ${raw.toFixed(2)}`
  if (mod.key === 'audio') return `${formatMetric(raw)} ${locale.value.startsWith('zh') ? '帧' : 'frames'}`
  if (mod.key === 'text') return `${formatMetric(raw, 0)} ${locale.value.startsWith('zh') ? '字符' : 'chars'}`
  if (mod.key === 'histograms') return `${formatMetric(raw, 0)} ${locale.value.startsWith('zh') ? '样本' : 'samples'}`
  if (mod.key === 'embeddings') return `${locale.value.startsWith('zh') ? '向量' : 'vector'} ${formatMetric(raw, 2)}`
  if (mod.key === 'prCurves') return `${locale.value.startsWith('zh') ? '精确率' : 'precision'} ${formatMetric(raw, 3)}`
  if (mod.key === 'hparams') return `${locale.value.startsWith('zh') ? '指标' : 'metric'} ${formatMetric(raw)}`
  if (mod.key === 'graphs') return activeArchitecture.value || uiText.value.missing
  if (mod.key === 'profiler') return `${formatMetric(raw)} ms`
  return formatMetric(raw)
}

function handleModuleCardPointer(event: PointerEvent, mod: VizModule) {
  setActiveModule(mod.key)
  const point = localPoint(event, 1, 1)
  const bars = moduleMiniBars(mod)
  if (!bars.length) return
  const index = clamp(Math.round((point.px / 100) * (bars.length - 1)), 0, bars.length - 1)
  const nextProbe = {
    key: mod.key,
    index,
    label: moduleProbeLabel(mod, index),
    value: formatProbeValue(mod, bars[index], index),
    x: clamp(62 + index * 5, 54, 88),
    y: 78,
  }
  if (sameMiniProbe(miniProbe.value, nextProbe)) return
  miniProbe.value = nextProbe
}

function clearMiniProbe() {
  miniProbe.value = null
}

function moduleMiniBars(mod: VizModule) {
  return normalizeToPercent(moduleSnapshot(mod.key).values, 0).slice(0, 6)
}

function handleModulePointer(event: PointerEvent) {
  const point = localPoint(event, 360, 220)
  updatePointerTarget(modulePointer, point.px, point.py)
  const preview = activeModule.value.preview
  if (moduleArtBars.value.length) {
    const bars = moduleArtBars.value
    const index = bars.reduce((closest, bar, barIndex) => {
      const center = bar.x + bar.width / 2
      const closestCenter = bars[closest].x + bars[closest].width / 2
      return Math.abs(center - point.x) < Math.abs(closestCenter - point.x) ? barIndex : closest
    }, 0)
    const bar = bars[index]
    const nextProbe = {
      kind: 'bar',
      index,
      label: moduleProbeLabel(activeModule.value, index),
      value: formatProbeValue(activeModule.value, moduleArtValues.value[index], index),
      x: clamp(((bar.x + bar.width / 2) / 360) * 100, 12, 88),
      y: clamp((bar.y / 220) * 100, 12, 76),
      svgX: bar.x + bar.width / 2,
      svgY: bar.y,
    }
    if (sameModuleProbe(moduleProbe.value, nextProbe)) return
    moduleProbe.value = nextProbe
    return
  }
  if (['embedding', 'graph', 'hparams'].includes(preview)) {
    const nodes = moduleArtNodes.value
    if (!nodes.length) return
    const index = nodes.reduce((closest, node, nodeIndex) => {
      const dist = Math.hypot(node.x - point.x, node.y - point.y)
      const closestDist = Math.hypot(nodes[closest].x - point.x, nodes[closest].y - point.y)
      return dist < closestDist ? nodeIndex : closest
    }, 0)
    const node = nodes[index]
    const raw = activeSnapshot.value.values[index] ?? node.r ?? 0
    const nextProbe = {
      kind: 'node',
      index,
      label: moduleProbeLabel(activeModule.value, index),
      value: formatProbeValue(activeModule.value, raw, index),
      x: clamp((node.x / 360) * 100, 12, 88),
      y: clamp((node.y / 220) * 100, 14, 76),
      svgX: node.x,
      svgY: node.y,
    }
    if (sameModuleProbe(moduleProbe.value, nextProbe)) return
    moduleProbe.value = nextProbe
    return
  }
  if (!moduleArtPoints.value.length) return
  const index = closestIndexByX(moduleArtPoints.value, point.x)
  const probePoint = moduleArtPoints.value[index]
  const nextProbe = {
    kind: 'curve',
    index,
    label: `${moduleProbeLabel(activeModule.value, index)} / ${uiText.value.step} ${index + 1}`,
    value: formatProbeValue(activeModule.value, moduleArtValues.value[index], index),
    x: clamp((probePoint.x / 360) * 100, 12, 88),
    y: clamp((probePoint.y / 220) * 100, 14, 76),
    svgX: probePoint.x,
    svgY: probePoint.y,
  }
  if (sameModuleProbe(moduleProbe.value, nextProbe)) return
  moduleProbe.value = nextProbe
}

function clearModuleProbe() {
  moduleProbe.value = null
}

function handleDetailPointer(event: PointerEvent) {
  const point = localPoint(event, 720, 360)
  updatePointerTarget(detailPointer, point.px, point.py)
  const mod = detailModule.value
  const preview = mod.preview

  if (detailBars.value.length) {
    const bars = detailBars.value
    const index = bars.reduce((closest, bar, barIndex) => {
      const center = bar.x + bar.width / 2
      const closestCenter = bars[closest].x + bars[closest].width / 2
      return Math.abs(center - point.x) < Math.abs(closestCenter - point.x) ? barIndex : closest
    }, 0)
    const bar = bars[index]
    const nextProbe = {
      kind: 'bar',
      index,
      label: moduleProbeLabel(mod, index),
      value: formatProbeValue(mod, detailValues.value[index], index),
      x: clamp(((bar.x + bar.width / 2) / 720) * 100, 8, 92),
      y: clamp((bar.y / 360) * 100, 12, 82),
      svgX: bar.x + bar.width / 2,
      svgY: bar.y,
    }
    if (sameModuleProbe(detailProbe.value, nextProbe)) return
    detailProbe.value = nextProbe
    return
  }

  if (['embedding', 'graph', 'hparams'].includes(preview)) {
    const nodes = detailNodes.value
    if (!nodes.length) return
    const index = nodes.reduce((closest, node, nodeIndex) => {
      const dist = Math.hypot(node.x - point.x, node.y - point.y)
      const closestDist = Math.hypot(nodes[closest].x - point.x, nodes[closest].y - point.y)
      return dist < closestDist ? nodeIndex : closest
    }, 0)
    const node = nodes[index]
    const raw = detailSnapshot.value.values[index] ?? node.r ?? 0
    const nextProbe = {
      kind: 'node',
      index,
      label: moduleProbeLabel(mod, index),
      value: formatProbeValue(mod, raw, index),
      x: clamp((node.x / 720) * 100, 8, 92),
      y: clamp((node.y / 360) * 100, 12, 82),
      svgX: node.x,
      svgY: node.y,
    }
    if (sameModuleProbe(detailProbe.value, nextProbe)) return
    detailProbe.value = nextProbe
    return
  }

  if (!detailPoints.value.length) return
  const index = closestIndexByX(detailPoints.value, point.x)
  const probePoint = detailPoints.value[index]
  const nextProbe = {
    kind: 'curve',
    index,
    label: `${moduleProbeLabel(mod, index)} / ${uiText.value.step} ${index + 1}`,
    value: formatProbeValue(mod, detailValues.value[index], index),
    x: clamp((probePoint.x / 720) * 100, 8, 92),
    y: clamp((probePoint.y / 360) * 100, 12, 82),
    svgX: probePoint.x,
    svgY: probePoint.y,
  }
  if (sameModuleProbe(detailProbe.value, nextProbe)) return
  detailProbe.value = nextProbe
}

function clearDetailProbe() {
  detailProbe.value = null
}

const detailHasCurve = computed(() => detailPoints.value.length > 0 && !detailBars.value.length && !['embedding', 'graph', 'hparams'].includes(detailModule.value.preview))

const handleRunChange = (val: number | null) => selectRun(val)
const activeRunName = computed(() => selectedJob.value?.name || (selectedRunId.value ? `run-${selectedRunId.value}` : uiText.value.noRun))
const chartInnerWidth = computed(() => chart.width - chart.left - chart.right)
const chartInnerHeight = computed(() => chart.height - chart.top - chart.bottom)
const baseline = computed(() => chart.top + chartInnerHeight.value)
const gridLines = computed(() => Array.from({ length: 5 }, (_, index) => chart.top + (chartInnerHeight.value / 4) * index))
const verticalTicks = computed(() => Array.from({ length: 6 }, (_, index) => chart.left + (chartInnerWidth.value / 5) * index))

function recordValue(record: ScalarRecord) {
  return Number(record?.value ?? 0)
}

function recordStep(record: ScalarRecord, index: number) {
  return Number(record?.step ?? index + 1)
}

function scalarRecords(tag: string) {
  return (scalarSeries.value[tag] || []).filter((record) => Number.isFinite(recordValue(record)))
}

function scalarValues(tag: string) {
  return scalarRecords(tag).map(recordValue)
}

function normalizeAccuracy(values: number[]) {
  const max = Math.max(...values, 0)
  return values.map((value) => max <= 1.5 ? value * 100 : value)
}

function latestValue(values: number[]) {
  return values.length ? values[values.length - 1] : null
}

function formatMetric(value: number | null, digits = 3) {
  if (value === null || !Number.isFinite(value)) return '--'
  if (Math.abs(value) >= 1000) return value.toLocaleString(undefined, { maximumFractionDigits: 0 })
  if (Math.abs(value) >= 10) return value.toFixed(1)
  if (Math.abs(value) >= 1) return value.toFixed(digits > 2 ? 2 : digits)
  return value.toFixed(digits)
}

function formatPercent(value: number | null) {
  if (value === null || !Number.isFinite(value)) return '--'
  return `${value.toFixed(1)}%`
}

const curveData = computed(() => {
  const lossRecords = scalarRecords('train/loss').length ? scalarRecords('train/loss') : scalarRecords('val/loss')
  const accuracyRecords = scalarRecords('train/accuracy').length ? scalarRecords('train/accuracy') : scalarRecords('val/accuracy')
  const baseRecords = lossRecords.length >= accuracyRecords.length ? lossRecords : accuracyRecords
  return {
    steps: baseRecords.map(recordStep),
    loss: lossRecords.map(recordValue),
    accuracy: normalizeAccuracy(accuracyRecords.map(recordValue)),
  }
})

function mapSeries(values: number[], min: number, max: number) {
  const span = max - min || 1
  return values.map((value, index) => ({
    x: chart.left + (index / Math.max(values.length - 1, 1)) * chartInnerWidth.value,
    y: chart.top + (1 - (value - min) / span) * chartInnerHeight.value,
  }))
}

function mapDetailSeries(values: number[], min: number, max: number) {
  const span = max - min || 1
  return values.map((value, index) => ({
    x: 34 + (index / Math.max(values.length - 1, 1)) * 652,
    y: 24 + (1 - (value - min) / span) * 276,
  }))
}

function toPath(points: Point[]) {
  return points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(' ')
}

function toArea(points: Point[]) {
  if (!points.length) return ''
  const first = points[0]
  const last = points[points.length - 1]
  return `${toPath(points)} L ${last.x.toFixed(1)} ${baseline.value.toFixed(1)} L ${first.x.toFixed(1)} ${baseline.value.toFixed(1)} Z`
}

function toDetailArea(points: Point[]) {
  if (!points.length) return ''
  const first = points[0]
  const last = points[points.length - 1]
  return `${toPath(points)} L ${last.x.toFixed(1)} 300 L ${first.x.toFixed(1)} 300 Z`
}

const lossPoints = computed(() => {
  if (!curveData.value.loss.length) return []
  const max = Math.max(...curveData.value.loss, 1.25)
  return mapSeries(curveData.value.loss, 0, max)
})
const accuracyPoints = computed(() => curveData.value.accuracy.length ? mapSeries(curveData.value.accuracy, 0, 100) : [])
const lossPath = computed(() => toPath(lossPoints.value))
const accuracyPath = computed(() => toPath(accuracyPoints.value))
const lossAreaPath = computed(() => toArea(lossPoints.value))
const accuracyAreaPath = computed(() => toArea(accuracyPoints.value))
const lastLossPoint = computed(() => lossPoints.value[lossPoints.value.length - 1] || { x: 0, y: 0 })
const lastAccuracyPoint = computed(() => accuracyPoints.value[accuracyPoints.value.length - 1] || { x: 0, y: 0 })
const lossMarkers = computed(() => lossPoints.value.filter((_, index) => index % 6 === 0))
const accuracyMarkers = computed(() => accuracyPoints.value.filter((_, index) => index % 6 === 3))

const distributionBars = computed(() => {
  const width = 320
  const height = 160
  const left = 18
  const right = 14
  const top = 16
  const bottom = 28
  const innerWidth = width - left - right
  const innerHeight = height - top - bottom
  const values = histogramCounts.value
  if (!values.length) return []
  const max = Math.max(...values, 1)
  const slot = innerWidth / values.length
  const barWidth = Math.max(slot * 0.62, 4)
  return values.map((value, index) => {
    const barHeight = (value / max) * innerHeight
    return {
      index,
      label: String(index - 9),
      value,
      x: left + index * slot + (slot - barWidth) / 2,
      y: top + innerHeight - barHeight,
      width: barWidth,
      height: barHeight,
      centerX: left + index * slot + slot / 2,
      centerY: top + innerHeight - barHeight,
      fill: index % 3 === 0 ? '#42e6a4' : index % 3 === 1 ? '#67e8f9' : '#a78bfa',
      opacity: index % 2 === 0 ? 0.96 : 0.74,
    }
  })
})

const distributionNodes = computed(() => distributionBars.value.map((bar) => ({ x: bar.centerX, y: bar.centerY })))
const distributionPolyline = computed(() => distributionNodes.value.map((node) => `${node.x.toFixed(1)},${node.y.toFixed(1)}`).join(' '))

function parseNumberArray(input: unknown): number[] {
  if (Array.isArray(input)) return input.map(Number).filter(Number.isFinite)
  if (typeof input !== 'string' || !input.trim()) return []
  try {
    const parsed = JSON.parse(input)
    if (Array.isArray(parsed)) return parsed.map(Number).filter(Number.isFinite)
    if (parsed && typeof parsed === 'object') return Object.values(parsed).map(Number).filter(Number.isFinite)
  } catch {
    return input.split(/[,\s]+/).map(Number).filter(Number.isFinite)
  }
  return []
}

function normalizeToPercent(values: number[], fallback = 12) {
  const clean = values.filter(Number.isFinite)
  const source = clean.length ? clean : Array.from({ length: fallback }, () => 0)
  const max = Math.max(...source.map((value) => Math.abs(value)), 1)
  return source.map((value) => clamp(Math.abs(value) / max * 100, 4, 100))
}

const latestHistogram = computed(() => {
  const records = modulePayloads.value.histograms as HistogramRecord[]
  return records[records.length - 1] || null
})

const histogramCounts = computed(() => {
  const counts = parseNumberArray(latestHistogram.value?.countsJson)
  return counts.length ? counts : []
})

const scalarStepCount = computed(() => Math.max(curveData.value.steps.length, curveData.value.loss.length, curveData.value.accuracy.length))
const latestLoss = computed(() => latestValue(curveData.value.loss))
const latestAccuracy = computed(() => latestValue(curveData.value.accuracy))
const latestLearningRate = computed(() => latestValue(scalarValues('train/learning_rate')))
const latestValLoss = computed(() => latestValue(scalarValues('val/loss')))
const latestValAccuracy = computed(() => latestValue(normalizeAccuracy(scalarValues('val/accuracy'))))

const previewMetrics = computed(() => [
  { label: locale.value.startsWith('zh') ? '轮次' : 'Epoch', value: String(scalarStepCount.value || '--'), detail: selectedJob.value?.status || selectedRunType.value, tone: 'var(--primary-color)' },
  { label: metricText('loss', locale).shortLabel, value: formatMetric(latestLoss.value), detail: latestValLoss.value !== null ? `${metricText('valLoss', locale).shortLabel} ${formatMetric(latestValLoss.value)}` : 'train/loss', tone: 'var(--tone-rose)' },
  { label: metricText('accuracy', locale).shortLabel, value: formatPercent(latestAccuracy.value), detail: latestValAccuracy.value !== null ? `${metricText('valAccuracy', locale).shortLabel} ${formatPercent(latestValAccuracy.value)}` : 'train/accuracy', tone: 'var(--tone-green)' },
  { label: metricText('learningRate', locale).shortLabel, value: formatMetric(latestLearningRate.value, 5), detail: 'train/learning_rate', tone: 'var(--tone-blue)' },
])

const signalCards = computed(() => [
  { label: metricText('loss', locale).shortLabel, value: formatMetric(latestLoss.value), color: 'var(--tone-rose)', points: normalizeToPercent(curveData.value.loss) },
  { label: metricText('accuracy', locale).shortLabel, value: formatPercent(latestAccuracy.value), color: 'var(--tone-green)', points: normalizeToPercent(curveData.value.accuracy) },
  { label: metricText('learningRate', locale).shortLabel, value: formatMetric(latestLearningRate.value, 5), color: 'var(--tone-blue)', points: normalizeToPercent(scalarValues('train/learning_rate')) },
])

function percentile(values: number[], ratio: number) {
  if (!values.length) return 0
  const sorted = [...values].sort((a, b) => a - b)
  return sorted[Math.min(sorted.length - 1, Math.max(0, Math.round((sorted.length - 1) * ratio)))]
}

function payloadSteps(records: Array<{ step?: number }>) {
  return records.map((record, index) => Number(record.step ?? index + 1))
}

function summarizeRecords(records: Array<{ step?: number }>) {
  const latest = records[records.length - 1]
  return [
    { label: uiText.value.records, value: String(records.length) },
    { label: uiText.value.latestStep, value: latest ? String(latest.step ?? records.length) : '--' },
  ]
}

function moduleSnapshot(moduleKey: string): ModuleSnapshot {
  if (moduleKey === 'scalars') {
    return {
      values: curveData.value.loss.length ? curveData.value.loss : curveData.value.accuracy,
      secondary: curveData.value.accuracy,
      steps: curveData.value.steps,
      rows: [
        { label: metricText('loss', locale).label, value: formatMetric(latestLoss.value) },
        { label: metricText('accuracy', locale).label, value: formatPercent(latestAccuracy.value) },
        { label: metricText('valLoss', locale).label, value: formatMetric(latestValLoss.value) },
        { label: metricText('learningRate', locale).label, value: formatMetric(latestLearningRate.value, 5) },
      ],
      summary: hasScalarData.value ? `${scalarStepCount.value} ${uiText.value.steps}` : uiText.value.emptyScalars,
      hasData: hasScalarData.value,
    }
  }

  if (moduleKey === 'histograms') {
    const records = modulePayloads.value.histograms as HistogramRecord[]
    const counts = histogramCounts.value
    return {
      values: counts,
      bars: counts,
      steps: payloadSteps(records),
      rows: [
        ...summarizeRecords(records),
        { label: uiText.value.bins, value: String(counts.length || '--') },
        { label: uiText.value.total, value: latestHistogram.value?.totalCount ? String(latestHistogram.value.totalCount) : '--' },
      ],
      summary: counts.length ? `${counts.length} ${uiText.value.bins}` : uiText.value.emptyModule,
      hasData: counts.length > 0,
    }
  }

  if (moduleKey === 'embeddings') {
    const records = modulePayloads.value.embeddings as EmbeddingRecord[]
    const nodes = records.map((record, index) => {
      const values = parseNumberArray(record.valuesJson)
      return {
        x: values[0] ?? index,
        y: values[1] ?? values[0] ?? 0,
        r: 4 + ((record.classId ?? index) % 4),
        label: record.label || record.sampleId ? String(record.label || record.sampleId) : `${locale.value.startsWith('zh') ? '样本' : 'sample'} ${index + 1}`,
        value: values[0] ?? 0,
      }
    })
    return {
      values: nodes.map((node) => Math.hypot(node.x, node.y)),
      nodes,
      steps: payloadSteps(records),
      rows: [
        ...summarizeRecords(records),
        { label: uiText.value.classes, value: String(new Set(records.map((item) => item.classId).filter((item) => item !== undefined)).size || '--') },
      ],
      summary: nodes.length ? `${nodes.length} ${uiText.value.projectedVectors}` : uiText.value.emptyModule,
      hasData: nodes.length > 0,
    }
  }

  if (moduleKey === 'prCurves') {
    const records = modulePayloads.value.prCurves as PRCurveRecord[]
    const latest = records[records.length - 1]
    const precision = parseNumberArray(latest?.precisionJson)
    const recall = parseNumberArray(latest?.recallJson)
    return {
      values: precision,
      secondary: recall,
      steps: recall.length ? recall : precision.map((_, index) => index + 1),
      rows: [
        ...summarizeRecords(records),
        { label: uiText.value.thresholds, value: String((latest?.numThresholds ?? precision.length) || '--') },
        { label: uiText.value.maxPrecision, value: formatMetric(precision.length ? Math.max(...precision) : null) },
      ],
      summary: precision.length ? `${precision.length} ${metricText('prCurve', locale).shortLabel}` : uiText.value.emptyModule,
      hasData: precision.length > 0,
    }
  }

  if (moduleKey === 'hparams') {
    const records = modulePayloads.value.hparams as HParamRecord[]
    const latest = records[records.length - 1]
    const metrics = parseKeyValueJson(latest?.metricValuesJson)
    const values = Object.values(metrics).map(Number).filter(Number.isFinite)
    const nodes = Object.entries(metrics).map(([label, value], index) => ({
      x: index,
      y: Number(value),
      r: 5 + (index % 3),
      label,
      value: Number(value),
    }))
    return {
      values,
      nodes,
      rows: Object.entries(metrics).slice(0, 4).map(([label, value]) => ({ label, value: formatMetric(Number(value)) })),
      summary: values.length ? `${values.length} ${uiText.value.hyperMetricCount}` : uiText.value.emptyModule,
      hasData: values.length > 0,
    }
  }

  if (moduleKey === 'graphs') {
    const architecture = activeArchitecture.value
    return {
      values: [],
      rows: [
        { label: uiText.value.architecture, value: architecture || '--' },
        { label: uiText.value.source, value: selectedRunType.value },
        { label: uiText.value.status, value: selectedJob.value?.status || '--' },
      ],
      summary: architecture
        ? uiText.value.graphMissing.replace('{model}', architecture)
        : uiText.value.emptyModule,
      hasData: false,
    }
  }

  if (moduleKey === 'profiler') {
    const records = modulePayloads.value.profiler as ProfilerRecord[]
    const latest = records[records.length - 1]
    const profiler = parseKeyValueJson(latest?.profilerJson)
    const values = Object.values(profiler).map(Number).filter(Number.isFinite)
    return {
      values,
      bars: values,
      steps: payloadSteps(records),
      rows: Object.entries(profiler).slice(0, 4).map(([label, value]) => ({ label, value: formatMetric(Number(value)) })),
      summary: values.length ? `${values.length} ${uiText.value.profilerSamples}` : uiText.value.emptyModule,
      hasData: values.length > 0,
    }
  }

  const records = normalizeRecords<any>(modulePayloads.value[moduleKey])
  const values = records.map((record, index) => Number(record.numFrames ?? record.width ?? record.height ?? record.text?.length ?? record.step ?? index + 1)).filter(Number.isFinite)
  return {
    values,
    bars: values,
    steps: payloadSteps(records),
    rows: summarizeRecords(records),
    summary: values.length ? `${values.length} ${uiText.value.realRecords}` : uiText.value.emptyModule,
    hasData: values.length > 0,
  }
}

function parseKeyValueJson(input: unknown): Record<string, number | string> {
  if (typeof input !== 'string' || !input.trim()) return {}
  try {
    const parsed = JSON.parse(input)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
    return parsed as Record<string, number | string>
  } catch {
    return {}
  }
}

const activeSnapshot = computed(() => moduleSnapshot(activeModule.value.key))
const detailSnapshot = computed(() => moduleSnapshot(detailModule.value.key))
const activeModuleStats = computed(() => activeModule.value.stats.map((stat, index) => {
  if (index === 0) return { label: uiText.value.records, value: String(activeSnapshot.value.values.length || activeSnapshot.value.nodes?.length || 0) }
  if (index === 1) return { label: uiText.value.status, value: activeSnapshot.value.hasData ? uiText.value.live : uiText.value.missing }
  return stat
}))
const detailModuleStats = computed(() => detailModule.value.stats.map((stat, index) => {
  if (index === 0) return { label: uiText.value.records, value: String(detailSnapshot.value.values.length || detailSnapshot.value.nodes?.length || 0) }
  if (index === 1) return { label: uiText.value.status, value: detailSnapshot.value.hasData ? uiText.value.live : uiText.value.missing }
  return stat
}))

const moduleArtValues = computed(() => {
  return normalizeToPercent(activeSnapshot.value.values, 0)
})

const moduleArtPoints = computed(() => {
  const values = moduleArtValues.value
  if (!values.length) return []
  return values.map((value, index) => ({
    x: 24 + (index / Math.max(values.length - 1, 1)) * 312,
    y: 188 - (value / 100) * 142,
  }))
})

const moduleArtPrimaryPath = computed(() => toPath(moduleArtPoints.value))
const moduleArtSecondaryPath = computed(() => {
  const secondary = activeSnapshot.value.secondary ? normalizeToPercent(activeSnapshot.value.secondary, 0) : []
  const points = secondary.length
    ? secondary.map((value, index) => ({
        x: 24 + (index / Math.max(secondary.length - 1, 1)) * 312,
        y: 188 - (value / 100) * 142,
      }))
    : []
  return toPath(points)
})
const moduleArtAreaPath = computed(() => {
  const points = moduleArtPoints.value
  if (!points.length) return ''
  const first = points[0]
  const last = points[points.length - 1]
  return `${toPath(points)} L ${last.x.toFixed(1)} 194 L ${first.x.toFixed(1)} 194 Z`
})

const moduleArtBars = computed(() => {
  const preview = activeModule.value.preview
  if (['curve', 'pr', 'embedding', 'graph'].includes(preview)) return []
  return moduleArtValues.value.slice(0, 12).map((value, index) => ({
    id: `${preview}-${index}`,
    index,
    x: 26 + index * 26,
    y: 188 - (value / 100) * 130,
    width: preview === 'text' ? 18 + (index % 3) * 5 : 14,
    height: (value / 100) * 130,
    radius: preview === 'image' ? 2 : 6,
  }))
})

const moduleArtNodes = computed(() => {
  const preview = activeModule.value.preview
  const snapshotNodes = activeSnapshot.value.nodes
  if (snapshotNodes?.length) {
    const xs = snapshotNodes.map((node) => node.x)
    const ys = snapshotNodes.map((node) => node.y)
    const minX = Math.min(...xs)
    const maxX = Math.max(...xs)
    const minY = Math.min(...ys)
    const maxY = Math.max(...ys)
    return snapshotNodes.slice(0, 40).map((node, index) => ({
      id: `${preview}-${index}`,
      index,
      x: 44 + ((node.x - minX) / (maxX - minX || 1)) * 268,
      y: 38 + ((node.y - minY) / (maxY - minY || 1)) * 142,
      r: node.r || 4 + (index % 3),
    }))
  }
  if (!['embedding', 'graph', 'hparams', 'image'].includes(preview)) {
    return moduleArtPoints.value.filter((_, index) => index % 3 === 0).map((point, index) => ({
      id: `point-${index}`,
      index,
      x: point.x,
      y: point.y,
      r: 3.4,
    }))
  }
  return []
})

const moduleArtEdges = computed(() => {
  const preview = activeModule.value.preview
  if (!activeSnapshot.value.hasData) return []
  if (!['graph', 'hparams', 'embedding'].includes(preview)) return []
  const nodes = moduleArtNodes.value
  if (!nodes.length) return []
  return nodes.slice(0, Math.max(nodes.length - 1, 0)).map((node, index) => {
    const next = nodes[(index + (preview === 'graph' ? 3 : 2)) % nodes.length]
    return {
      id: `${preview}-edge-${index}`,
      x1: node.x,
      y1: node.y,
      x2: next.x,
      y2: next.y,
    }
  })
})

const detailGridY = computed(() => Array.from({ length: 6 }, (_, index) => 24 + (276 / 5) * index))
const detailGridX = computed(() => Array.from({ length: 7 }, (_, index) => 34 + (652 / 6) * index))

const detailValues = computed(() => {
  return normalizeToPercent(detailSnapshot.value.values, 0)
})

const detailPoints = computed(() => {
  const values = detailValues.value
  if (!values.length) return []
  return mapDetailSeries(values, 0, 100)
})

const detailPrimaryPath = computed(() => toPath(detailPoints.value))
const detailSecondaryPath = computed(() => {
  const secondary = detailSnapshot.value.secondary ? normalizeToPercent(detailSnapshot.value.secondary, 0) : []
  const points = secondary.length ? mapDetailSeries(secondary, 0, 100) : []
  return toPath(points)
})
const detailAreaPath = computed(() => toDetailArea(detailPoints.value))

const detailBars = computed(() => {
  const preview = detailModule.value.preview
  if (!['image', 'audio', 'text', 'histogram', 'profiler'].includes(preview)) return []
  const values = normalizeToPercent(detailSnapshot.value.bars || detailSnapshot.value.values, 0)
  if (!values.length) return []
  const slot = 652 / values.length
  const width = preview === 'profiler' ? Math.max(slot * 0.92, 8) : Math.max(slot * 0.64, 6)
  return values.map((value, index) => {
    const height = (value / 100) * 240
    return {
      id: `${detailModule.value.key}-${index}`,
      index,
      x: 34 + index * slot + (slot - width) / 2,
      y: 300 - height,
      width,
      height,
      radius: preview === 'image' ? 3 : preview === 'profiler' ? 2 : 7,
    }
  })
})

const detailNodes = computed(() => {
  const preview = detailModule.value.preview
  const snapshotNodes = detailSnapshot.value.nodes
  if (snapshotNodes?.length) {
    const xs = snapshotNodes.map((node) => node.x)
    const ys = snapshotNodes.map((node) => node.y)
    const minX = Math.min(...xs)
    const maxX = Math.max(...xs)
    const minY = Math.min(...ys)
    const maxY = Math.max(...ys)
    return snapshotNodes.slice(0, 80).map((node, index) => ({
      id: `${detailModule.value.key}-node-${index}`,
      index,
      x: 70 + ((node.x - minX) / (maxX - minX || 1)) * 580,
      y: 48 + ((node.y - minY) / (maxY - minY || 1)) * 226,
      r: node.r || 4 + (index % 3),
    }))
  }
  if (!['embedding', 'graph', 'hparams'].includes(preview)) {
    return detailPoints.value.filter((_, index) => index % 4 === 0).map((point, index) => ({
      id: `detail-point-${index}`,
      index,
      x: point.x,
      y: point.y,
      r: 4.2,
    }))
  }
  return []
})

const detailEdges = computed(() => {
  const preview = detailModule.value.preview
  if (!detailSnapshot.value.hasData) return []
  if (!['embedding', 'graph', 'hparams'].includes(preview)) return []
  const nodes = detailNodes.value
  if (!nodes.length) return []
  return nodes.slice(0, Math.max(nodes.length - 1, 0)).map((node, index) => {
    const next = nodes[(index + (preview === 'graph' ? 4 : preview === 'hparams' ? 5 : 3)) % nodes.length]
    return {
      id: `${detailModule.value.key}-detail-edge-${index}`,
      x1: node.x,
      y1: node.y,
      x2: next.x,
      y2: next.y,
    }
  })
})

const detailAxisLabels = computed(() => {
  const isZh = locale.value.startsWith('zh')
  if (detailModule.value.key === 'prCurves') return [isZh ? '召回率 0.00' : 'recall 0.00', isZh ? '召回率 1.00' : 'recall 1.00']
  if (detailModule.value.key === 'profiler') return [isZh ? '算子开始' : 'op start', isZh ? '单步预算' : 'step budget']
  if (detailModule.value.key === 'histograms') return [isZh ? '区间 -3σ' : 'bin -3σ', isZh ? '区间 +3σ' : 'bin +3σ']
  if (detailModule.value.key === 'embeddings') return [isZh ? '聚类空间' : 'cluster space', isZh ? '近邻' : 'neighbors']
  if (detailModule.value.key === 'graphs') return [isZh ? '输入层' : 'input layers', isZh ? '输出头' : 'output head']
  return [`${uiText.value.step} 1`, `${uiText.value.step} ${detailValues.value.length}`]
})

const detailSummary = computed(() => {
  const latest = detailValues.value[detailValues.value.length - 1] || 0
  return {
    primary: detailSnapshot.value.hasData ? formatProbeValue(detailModule.value, latest, detailValues.value.length - 1) : uiText.value.missing,
    secondary: detailSnapshot.value.summary || `${detailModule.value.stage} ${uiText.value.live}`,
  }
})

const detailRows = computed(() => {
  if (detailSnapshot.value.rows?.length) {
    return [
      { label: uiText.value.source, value: selectedRunType.value },
      ...detailSnapshot.value.rows,
    ].slice(0, 5)
  }
  const values = detailValues.value
  if (!values.length) {
    return [
      { label: uiText.value.source, value: selectedRunType.value },
      { label: uiText.value.status, value: dataStatusLabel.value },
      { label: uiText.value.records, value: '0' },
    ]
  }
  const max = Math.max(...values)
  const min = Math.min(...values)
  const avg = values.reduce((sum, value) => sum + value, 0) / Math.max(values.length, 1)
  return [
    { label: uiText.value.selected, value: detailProbe.value?.label || moduleProbeLabel(detailModule.value, 0) },
    { label: uiText.value.peak, value: formatProbeValue(detailModule.value, max, values.indexOf(max)) },
    { label: uiText.value.low, value: formatProbeValue(detailModule.value, min, values.indexOf(min)) },
    { label: uiText.value.mean, value: formatProbeValue(detailModule.value, avg, 0) },
  ]
})

const componentNodes = computed(() => [
  { stage: locale.value.startsWith('zh') ? '训练' : 'Train', name: metricText('loss', locale).shortLabel, bars: normalizeToPercent(curveData.value.loss) },
  { stage: locale.value.startsWith('zh') ? '训练' : 'Train', name: metricText('accuracy', locale).shortLabel, bars: normalizeToPercent(curveData.value.accuracy) },
  { stage: locale.value.startsWith('zh') ? '验证' : 'Val', name: metricText('valLoss', locale).shortLabel, bars: normalizeToPercent(scalarValues('val/loss')) },
  { stage: metricText('learningRate', locale).shortLabel, name: locale.value.startsWith('zh') ? '调度' : 'Schedule', bars: normalizeToPercent(scalarValues('train/learning_rate')) },
])
</script>

<style scoped>
.vizhub-page {
  position: relative;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.selector-card,
.module-card,
.preview-panel,
.metric-card,
.signal-card {
  border-radius: var(--radius-lg);
}

.selector-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
}

.selector-row label {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.run-select {
  width: min(320px, 100%);
}

.analysis-mode-switch {
  position: relative;
  align-self: center;
  display: grid;
  grid-template-columns: repeat(2, minmax(118px, 1fr));
  min-width: min(340px, 100%);
  padding: 4px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 18%, var(--border-color));
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.14);
  overflow: hidden;
}

.analysis-mode-switch i {
  position: absolute;
  inset: 4px auto 4px 4px;
  width: calc(50% - 4px);
  border-radius: 999px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--active-accent) 32%, transparent), rgba(66, 230, 164, 0.16)),
    rgba(var(--glass-bg-rgb), 0.24);
  box-shadow: 0 12px 30px color-mix(in srgb, var(--active-accent) 18%, transparent);
  transition: transform 320ms cubic-bezier(0.22, 0.61, 0.36, 1);
}

.analysis-mode-switch button {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 2px;
  border: 0;
  padding: 8px 14px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  text-align: center;
}

.analysis-mode-switch button b,
.analysis-mode-switch button small {
  display: block;
}

.analysis-mode-switch button b {
  color: inherit;
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.analysis-mode-switch button small {
  font-size: 9px;
  font-weight: var(--font-weight-label);
  text-transform: uppercase;
}

.analysis-mode-switch button.active {
  color: var(--text-primary);
}

.matrix-action-stack {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 260px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

.analysis-module-ribbon {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.analysis-module-ribbon button,
.analysis-module-ribbon em {
  min-height: 46px;
  border: 1px solid color-mix(in srgb, var(--module-accent, var(--active-accent)) 22%, var(--border-color));
  border-radius: 16px;
  padding: 8px 10px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--module-accent, var(--active-accent)) 14%, transparent), transparent 62%),
    rgba(var(--glass-bg-rgb), 0.22);
}

.analysis-module-ribbon button {
  cursor: pointer;
  text-align: left;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.analysis-module-ribbon button:hover {
  border-color: color-mix(in srgb, var(--module-accent) 42%, var(--border-color));
  box-shadow: 0 14px 30px color-mix(in srgb, var(--module-accent) 12%, transparent);
  transform: translateY(-2px);
}

.analysis-module-ribbon span,
.analysis-module-ribbon b,
.analysis-module-ribbon em {
  display: block;
}

.analysis-module-ribbon span {
  color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 74%, var(--text-muted));
  font-size: 9px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.analysis-module-ribbon b {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.analysis-module-ribbon em {
  width: 100%;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-body);
  line-height: 1.5;
}

.analysis-action-row {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 16%, var(--border-color));
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--surface-2) 72%, transparent);
}

.matrix-action-stack em {
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-label);
  text-align: right;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix-controls {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(320px, 1.1fr);
  gap: 16px;
}

.matrix-control-card,
.matrix-result-card {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: color-mix(in srgb, var(--active-accent) 18%, transparent);
  --edge-sensitivity: 24;
  border: 1px solid color-mix(in srgb, var(--active-accent) 8%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.064), rgba(255, 255, 255, 0.018) 42%, rgba(0, 0, 0, 0.08)),
    rgba(var(--glass-bg-rgb), 0.34);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.matrix-control-card::before,
.matrix-result-card::before,
.result-filter-panel::before,
.result-group-panel::before,
.preview-panel::before,
.metric-card::before,
.signal-card::before,
.component-node::before,
.module-analysis-strip::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent) top / 100% 1px no-repeat;
  opacity: 0;
  transition: opacity 420ms ease;
}

.matrix-control-card::after,
.matrix-result-card::after,
.result-filter-panel::after,
.result-group-panel::after,
.preview-panel::after,
.metric-card::after,
.signal-card::after,
.module-analysis-strip::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  padding: var(--edge-glow-width, 1.35px);
  border: 0;
  border-radius: inherit;
  background:
    linear-gradient(rgb(0 0 0 / 0), rgb(0 0 0 / 0)) padding-box,
    var(--edge-glow-border);
  opacity: 0;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, calc(var(--edge-glow-opacity) * 0.12)),
    0 0 calc(20px * var(--edge-glow-opacity)) var(--edge-glow-soft);
  filter: saturate(1.08) brightness(1.03);
  mix-blend-mode: screen;
  transition: opacity 120ms ease, box-shadow 120ms ease, filter 120ms ease;
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  mask-composite: exclude;
}

.matrix-control-card:hover::before,
.matrix-result-card:hover::before,
.result-filter-panel:hover::before,
.result-group-panel:hover::before,
.preview-panel:hover::before,
.metric-card:hover::before,
.signal-card:hover::before,
.component-node:hover::before,
.module-analysis-strip:hover::before,
.matrix-control-card:focus-within::before,
.matrix-result-card:focus-within::before,
.result-filter-panel:focus-within::before,
.result-group-panel:focus-within::before,
.preview-panel:focus-within::before,
.metric-card:focus-within::before,
.signal-card:focus-within::before,
.component-node:focus-within::before,
.module-analysis-strip:focus-within::before {
  opacity: 0.24;
}

.matrix-control-card:hover::after,
.matrix-result-card:hover::after,
.result-filter-panel:hover::after,
.result-group-panel:hover::after,
.preview-panel:hover::after,
.metric-card:hover::after,
.signal-card:hover::after,
.module-analysis-strip:hover::after,
.matrix-control-card:focus-within::after,
.matrix-result-card:focus-within::after,
.result-filter-panel:focus-within::after,
.result-group-panel:focus-within::after,
.preview-panel:focus-within::after,
.metric-card:focus-within::after,
.signal-card:focus-within::after,
.module-analysis-strip:focus-within::after,
.matrix-control-card.edge-glow-active::after,
.matrix-result-card.edge-glow-active::after,
.result-filter-panel.edge-glow-active::after,
.result-group-panel.edge-glow-active::after,
.preview-panel.edge-glow-active::after,
.metric-card.edge-glow-active::after,
.signal-card.edge-glow-active::after,
.module-analysis-strip.edge-glow-active::after {
  opacity: var(--edge-glow-opacity);
}

.matrix-control-card > *,
.matrix-result-card > *,
.result-filter-panel > *,
.result-group-panel > *,
.preview-panel > *,
.metric-card > *,
.signal-card > *,
.component-node > *,
.module-analysis-strip > * {
  position: relative;
  z-index: 1;
}

.matrix-control-card {
  padding: 14px;
}

.focus-run-card p {
  margin: 0 0 14px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-body);
  line-height: 1.65;
}

.matrix-control-head,
.matrix-result-head,
.result-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.matrix-control-head {
  margin-bottom: 12px;
}

.matrix-control-head span,
.matrix-result-head span,
.result-card-top span,
.ai-panel-slot span {
  display: block;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.matrix-control-head strong,
.matrix-result-head strong,
.result-card-top strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.matrix-check-grid,
.matrix-module-grid {
  display: grid;
  gap: 8px;
}

.matrix-check-grid :deep(.el-checkbox),
.matrix-module-grid :deep(.el-checkbox) {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 16%, transparent);
  height: auto;
  min-height: 48px;
  margin: 0;
  padding: 9px 11px;
  border-radius: 14px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--module-accent, var(--active-accent)) 6%, transparent), transparent 60%),
    rgba(var(--glass-bg-rgb), 0.26);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease, background 180ms ease;
}

.matrix-check-grid :deep(.el-checkbox::before),
.matrix-module-grid :deep(.el-checkbox::before) {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  border-radius: inherit;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--module-accent, var(--active-accent)) 18%, transparent), transparent) top / 100% 1px no-repeat;
  opacity: 0;
  transition: opacity 420ms ease;
}

.matrix-check-grid :deep(.el-checkbox:hover::before),
.matrix-module-grid :deep(.el-checkbox:hover::before),
.matrix-check-grid :deep(.el-checkbox.is-checked::before),
.matrix-module-grid :deep(.el-checkbox.is-checked::before) {
  opacity: 0.42;
}

.matrix-check-grid :deep(.el-checkbox > *),
.matrix-module-grid :deep(.el-checkbox > *) {
  position: relative;
  z-index: 1;
}

.matrix-check-grid :deep(.el-checkbox:hover),
.matrix-module-grid :deep(.el-checkbox:hover) {
  border-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 36%, var(--border-color));
  transform: translateY(-1px);
}

.matrix-check-grid :deep(.el-checkbox.is-checked),
.matrix-module-grid :deep(.el-checkbox.is-checked) {
  border-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 58%, var(--border-color));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--module-accent, var(--active-accent)) 14%, transparent), transparent 66%),
    rgba(var(--glass-bg-rgb), 0.34);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--module-accent, var(--active-accent)) 20%, transparent);
}

.matrix-check-grid :deep(.el-checkbox__label),
.matrix-module-grid :deep(.el-checkbox__label) {
  display: grid;
  gap: 3px;
  min-width: 0;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-label);
}

.matrix-run-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix-check-grid em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-body);
}

.matrix-module-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.one-many-module-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.matrix-module-grid small {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-body);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix-module-grid i {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 3px;
  font-style: normal;
}

.matrix-module-grid b {
  padding: 2px 5px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--module-accent, var(--active-accent)) 12%, transparent);
  color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 72%, white);
  font-size: 9px;
  font-weight: var(--font-weight-label);
}

.matrix-results {
  position: relative;
  z-index: 1;
  margin-top: 16px;
}

.matrix-result-head {
  margin-bottom: 12px;
  padding: 0 2px;
}

.result-filter-panel {
  position: relative;
  isolation: isolate;
  overflow: visible;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: color-mix(in srgb, var(--active-accent) 16%, transparent);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 16%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 8% 0%, color-mix(in srgb, var(--active-accent) 10%, transparent), transparent 36%),
    rgba(var(--glass-bg-rgb), 0.28);
}

.result-view-switch,
.result-filter-actions,
.result-filter-chips,
.result-persist-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.result-view-switch {
  padding: 4px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 14%, var(--border-color));
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.12);
}

.result-view-switch button,
.smart-select-action,
.clear-select-action,
.select-visible-action,
.save-view-action,
.save-selected-action,
.import-chat-action,
.result-filter-chips button {
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  font-size: 10px;
  font-weight: var(--font-weight-title);
  transition: transform 160ms ease, background 160ms ease, color 160ms ease, box-shadow 160ms ease;
}

.result-view-switch button {
  min-height: 34px;
  padding: 8px 14px;
  background: transparent;
  color: var(--text-muted);
}

.result-view-switch button.active {
  background: color-mix(in srgb, var(--active-accent) 20%, rgba(var(--glass-bg-rgb), 0.22));
  color: var(--text-primary);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--active-accent) 12%, transparent);
}

.smart-select-action,
.clear-select-action,
.select-visible-action,
.save-view-action,
.save-selected-action,
.import-chat-action {
  min-height: 36px;
  padding: 8px 14px;
}

.smart-select-action {
  background: linear-gradient(135deg, color-mix(in srgb, var(--active-accent) 28%, transparent), rgba(66, 230, 164, 0.12));
  color: color-mix(in srgb, var(--active-accent) 72%, white);
}

.clear-select-action {
  background: rgba(148, 163, 184, 0.1);
  color: var(--text-secondary);
}

.result-persist-actions {
  width: 100%;
  padding-top: 4px;
}

.view-save-panel {
  width: 100%;
  display: grid;
  gap: 12px;
  margin-top: 2px;
  padding: 12px;
  border: 1px solid rgba(66, 230, 164, 0.2);
  border-radius: 18px;
  background:
    radial-gradient(circle at 14% 0%, rgba(66, 230, 164, 0.14), transparent 34%),
    rgba(8, 16, 20, 0.56);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.view-save-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.view-save-head strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.view-save-head span {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 11px;
  line-height: 1.5;
}

.view-save-head button {
  flex: 0 0 auto;
  min-height: 34px;
  padding: 0 13px;
  border-radius: 999px;
  background: rgba(66, 230, 164, 0.12);
  color: #42e6a4;
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.view-save-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 9px;
  align-items: end;
}

.view-save-grid label {
  display: grid;
  gap: 6px;
}

.view-save-grid span {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.view-save-grid select {
  height: 36px;
  border: 1px solid rgba(66, 230, 164, 0.18);
  border-radius: 12px;
  background: rgba(var(--glass-bg-rgb), 0.34);
  color: var(--text-primary);
  padding: 0 10px;
  outline: none;
}

.view-save-grid > button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(66, 230, 164, 0.24), rgba(var(--primary-rgb), 0.14));
  color: #42e6a4;
  font-size: 11px;
  font-weight: var(--font-weight-title);
}

.result-persist-actions em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
}

.select-visible-action {
  border-color: rgba(var(--primary-rgb), 0.18);
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
}

.save-view-action,
.save-selected-action {
  border-color: rgba(66, 230, 164, 0.24);
  background: linear-gradient(135deg, rgba(66, 230, 164, 0.18), rgba(var(--primary-rgb), 0.1));
  color: #42e6a4;
}

.save-view-action.strong {
  border-color: rgba(66, 230, 164, 0.42);
  box-shadow: 0 0 0 1px rgba(66, 230, 164, 0.08), 0 14px 34px rgba(66, 230, 164, 0.08);
}

.import-chat-action {
  border-color: rgba(251, 191, 36, 0.26);
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.18), rgba(var(--primary-rgb), 0.1));
  color: #fbbf24;
}

.result-persist-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}

.smart-select-action:hover,
.clear-select-action:hover,
.result-filter-chips button:hover {
  transform: translateY(-1px);
}

.result-filter-chips {
  width: 100%;
  padding-top: 2px;
}

.result-filter-chips button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 7px 11px 7px 13px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(var(--glass-bg-rgb), 0.22);
  color: var(--text-secondary);
}

.result-filter-chips button.active {
  border-color: color-mix(in srgb, var(--active-accent) 52%, var(--border-color));
  background: color-mix(in srgb, var(--active-accent) 16%, rgba(var(--glass-bg-rgb), 0.28));
  color: var(--text-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--active-accent) 16%, transparent);
}

.result-filter-chips b {
  display: inline-grid;
  min-width: 20px;
  min-height: 20px;
  place-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: color-mix(in srgb, var(--active-accent) 72%, white);
  font-size: 9px;
  font-weight: var(--font-weight-title);
}

.result-group-stack {
  display: grid;
  gap: 14px;
}

.result-group-panel {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: color-mix(in srgb, var(--active-accent) 14%, transparent);
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 14%, var(--border-color));
  border-radius: var(--radius-lg);
  background: rgba(var(--glass-bg-rgb), 0.18);
}

.result-group-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.result-group-head span,
.result-group-head em {
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.result-group-head strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: var(--font-weight-title);
}

.matrix-result-head em {
  color: var(--text-muted);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-label);
}

.matrix-result-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.matrix-result-card {
  position: relative;
  padding: 14px;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.matrix-result-card:hover {
  border-color: rgba(var(--primary-rgb), 0.34);
  box-shadow: 0 16px 42px rgba(var(--primary-rgb), 0.1);
  transform: translateY(-2px);
}

.matrix-result-card.empty {
  opacity: 0.74;
}

.matrix-result-card.selected {
  border-color: color-mix(in srgb, var(--active-accent) 72%, #42e6a4);
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--active-accent) 34%, transparent),
    0 18px 46px color-mix(in srgb, var(--active-accent) 16%, transparent);
}

.result-card-select {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  padding: 5px 9px;
  background: rgba(2, 6, 23, 0.48);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 9px;
  font-weight: var(--font-weight-title);
  backdrop-filter: blur(10px);
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease, color 160ms ease;
}

.result-card-select:hover {
  transform: translateY(-1px);
  border-color: rgba(var(--primary-rgb), 0.34);
  color: var(--primary-color);
}

.result-card-select.active {
  border-color: color-mix(in srgb, var(--active-accent) 68%, #42e6a4);
  background: color-mix(in srgb, var(--active-accent) 24%, rgba(2, 6, 23, 0.58));
  color: #ecfff7;
}

.result-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}

.result-status-row span {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-label);
}

.result-score {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin: 14px 0 8px;
}

.result-score strong {
  color: var(--primary-color);
  font-size: 24px;
  font-weight: var(--font-weight-title);
  line-height: 1;
}

.result-score em,
.matrix-result-card p,
.ai-panel-slot p {
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-body);
  line-height: 1.55;
}

.matrix-result-card p {
  min-height: 34px;
  margin: 0 0 12px;
}

.ai-panel-slot {
  padding: 11px;
  border: 1px solid rgba(var(--primary-rgb), 0.18);
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.1), transparent 62%),
    rgba(0, 0, 0, 0.08);
}

.ai-panel-slot strong {
  display: block;
  margin-top: 5px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.ai-panel-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ai-panel-meta em,
.ai-panel-meta b {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  border-radius: 999px;
  padding: 3px 7px;
  font-size: 9px;
  font-style: normal;
  font-weight: var(--font-weight-title);
  line-height: 1;
}

.ai-panel-meta em.is-ai {
  background: rgba(66, 230, 164, 0.14);
  color: #42e6a4;
}

.ai-panel-meta em.is-rule {
  background: rgba(251, 191, 36, 0.14);
  color: #fbbf24;
}

.ai-panel-meta b {
  max-width: 100%;
  background: rgba(var(--primary-rgb), 0.1);
  color: color-mix(in srgb, var(--primary-color) 78%, white);
}

.ai-panel-slot p {
  min-height: auto;
  margin: 7px 0 10px;
  white-space: pre-line;
}

.ai-recommendations {
  display: grid;
  gap: 5px;
  margin: 0 0 10px;
}

.ai-recommendations b {
  padding: 6px 8px;
  border-radius: 10px;
  background: rgba(var(--primary-rgb), 0.08);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-label);
  line-height: 1.45;
}

.ai-panel-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.ai-panel-actions button {
  border: 0;
  border-radius: 999px;
  padding: 6px 10px;
  background: rgba(var(--primary-rgb), 0.14);
  color: var(--primary-color);
  cursor: pointer;
  font-size: 10px;
  font-weight: var(--font-weight-title);
  transition: transform 140ms ease, border-color 140ms ease, background 140ms ease;
}

.ai-panel-actions button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.ai-panel-actions .model-action {
  background: linear-gradient(135deg, rgba(66, 230, 164, 0.2), rgba(var(--primary-rgb), 0.12));
  color: #42e6a4;
}

.ai-panel-actions .rule-action {
  background: rgba(251, 191, 36, 0.14);
  color: #fbbf24;
}

.ai-panel-actions button:disabled {
  cursor: wait;
  opacity: 0.62;
}

.matrix-result-enter-active,
.matrix-result-leave-active {
  transition: opacity 220ms ease, transform 220ms ease;
}

.matrix-result-enter-from,
.matrix-result-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

.header-index {
  min-width: 108px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--surface-2);
  text-align: right;
}

.header-index span {
  display: block;
  color: var(--primary-color);
  font-size: 28px;
  font-weight: var(--font-weight-title);
  line-height: 1;
}

.header-index small {
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.workspace-band {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: rgba(255, 255, 255, 0.1);
  --edge-sensitivity: 24;
  margin-bottom: 22px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 8%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.018) 42%, rgba(0, 0, 0, 0.08)),
    color-mix(in srgb, var(--surface-1) 70%, transparent);
  box-shadow: var(--shadow-soft);
}

.workspace-band::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent) top / 100% 1px no-repeat;
  opacity: 0.24;
  transition: opacity 420ms ease;
}

.workspace-band::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  padding: var(--edge-glow-width, 1.35px);
  border: 0;
  border-radius: inherit;
  background:
    linear-gradient(rgb(0 0 0 / 0), rgb(0 0 0 / 0)) padding-box,
    var(--edge-glow-border);
  opacity: 0;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, calc(var(--edge-glow-opacity) * 0.1)),
    0 0 calc(20px * var(--edge-glow-opacity)) var(--edge-glow-soft);
  filter: saturate(1.08) brightness(1.03);
  mix-blend-mode: screen;
  transition: opacity 120ms ease, box-shadow 120ms ease, filter 120ms ease;
  -webkit-mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#000 0 0) content-box,
    linear-gradient(#000 0 0);
  mask-composite: exclude;
}

.workspace-band:hover::before,
.workspace-band:focus-within::before {
  opacity: 0.28;
}

.workspace-band:hover::after,
.workspace-band:focus-within::after,
.workspace-band.edge-glow-active::after {
  opacity: var(--edge-glow-opacity);
}

.band-caption {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.band-caption.compact {
  margin-bottom: 12px;
}

.band-caption span {
  display: block;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.band-caption strong {
  display: block;
  margin-top: 5px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: var(--font-weight-title);
}

.band-caption p {
  max-width: 760px;
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-body);
  line-height: 1.65;
}

.preview-stage {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 16px;
}

.preview-panel,
.metric-card,
.signal-card {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: rgba(var(--primary-rgb), 0.14);
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
}

.preview-panel {
  padding: 16px;
}

.panel-head,
.section-caption {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head span,
.section-caption span,
.metric-card span,
.signal-card span {
  display: block;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.panel-head strong,
.section-caption strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: var(--font-weight-title);
}

.panel-head em {
  padding: 4px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.24);
  border-radius: var(--radius-sm);
  color: var(--primary-color);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
}

.preview-side {
  display: grid;
  gap: 16px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  min-height: 92px;
  padding: 14px;
}

.metric-card strong {
  display: block;
  margin-top: 9px;
  font-size: 24px;
  font-weight: var(--font-weight-title);
  line-height: 1;
}

.metric-card em {
  display: block;
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-body);
}

.chart-box {
  position: relative;
  width: 100%;
  min-height: 160px;
  overflow: hidden;
}

.chart-empty-state {
  position: absolute;
  inset: 16px;
  z-index: 4;
  display: grid;
  place-content: center;
  gap: 7px;
  padding: 18px;
  border: 1px dashed color-mix(in srgb, var(--primary-color) 26%, var(--border-color));
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, rgba(var(--primary-rgb), 0.1), transparent 58%),
    rgba(var(--glass-bg-rgb), 0.34);
  color: var(--text-secondary);
  text-align: center;
  pointer-events: none;
  backdrop-filter: blur(10px);
}

.chart-empty-state.compact {
  inset: 12px;
  padding: 12px;
}

.chart-empty-state strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.chart-empty-state span {
  max-width: 34ch;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: var(--font-weight-body);
  line-height: 1.55;
}

.interactive-chart,
.interactive-card {
  cursor: crosshair;
}

.curve-chart {
  min-height: 330px;
  border: 1px solid color-mix(in srgb, var(--primary-color) 12%, var(--border-color));
  background:
    linear-gradient(135deg, rgba(66, 230, 164, 0.07), transparent 44%),
    linear-gradient(180deg, rgba(251, 113, 133, 0.05), transparent 68%),
    rgba(var(--glass-bg-rgb), 0.22);
}

.distribution-chart {
  min-height: 178px;
  border: 1px solid color-mix(in srgb, var(--tone-blue) 14%, var(--border-color));
  background:
    linear-gradient(135deg, rgba(103, 232, 249, 0.08), transparent 52%),
    rgba(var(--glass-bg-rgb), 0.18);
}

.overview-svg,
.distribution-svg {
  display: block;
  width: 100%;
  height: 100%;
  min-height: inherit;
  overflow: hidden;
}

.plot-bg,
.distribution-bg {
  fill: rgba(255, 255, 255, 0.025);
  stroke: rgba(148, 163, 184, 0.16);
  stroke-width: 1;
}

.chart-grid line,
.vertical-grid line,
.distribution-axis {
  stroke: rgba(148, 163, 184, 0.34);
  stroke-width: 1;
  vector-effect: non-scaling-stroke;
}

.vertical-grid line {
  opacity: 0.36;
}

.loss-line,
.accuracy-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
  filter: drop-shadow(0 0 10px rgba(66, 230, 164, 0.18));
  animation: draw-curve 820ms cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.loss-line {
  stroke: #fb7185;
  filter: drop-shadow(0 0 10px rgba(251, 113, 133, 0.2));
}

.accuracy-line {
  stroke: #42e6a4;
}

.loss-dot {
  fill: #fb7185;
  stroke: var(--surface-solid);
  stroke-width: 2;
}

.accuracy-dot {
  fill: #42e6a4;
  stroke: var(--surface-solid);
  stroke-width: 2;
}

.curve-markers circle {
  stroke: rgba(13, 17, 24, 0.72);
  stroke-width: 1.6;
}

.axis-labels text {
  fill: color-mix(in srgb, var(--text-muted) 84%, #42e6a4);
  font-size: 10px;
  font-weight: var(--font-weight-body);
}

.chart-legend {
  position: absolute;
  right: 14px;
  bottom: 12px;
  display: inline-flex;
  gap: 12px;
  padding: 6px 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--surface-1) 74%, transparent);
}

.chart-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-title);
}

.chart-legend i {
  width: 18px;
  height: 3px;
  border-radius: 999px;
}

.chart-explainer {
  position: absolute;
  left: 16px;
  right: 150px;
  top: 14px;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 760;
  line-height: 1.55;
  pointer-events: none;
}

.loss-key {
  background: #fb7185;
}

.accuracy-key {
  background: #42e6a4;
}

.distribution-svg rect {
  filter: drop-shadow(0 7px 12px rgba(66, 230, 164, 0.14));
  transition: opacity 140ms ease, transform 140ms ease;
  transform-box: fill-box;
  transform-origin: center bottom;
}

.distribution-svg rect.active {
  opacity: 1;
  transform: scaleY(1.08);
}

.distribution-svg polyline {
  filter: drop-shadow(0 0 7px rgba(251, 113, 133, 0.22));
}

.chart-probe {
  pointer-events: none;
}

.probe-line {
  stroke: color-mix(in srgb, var(--primary-color) 64%, transparent);
  stroke-width: 1.1;
  stroke-dasharray: 4 6;
  vector-effect: non-scaling-stroke;
}

.probe-line.faint {
  stroke: rgba(148, 163, 184, 0.45);
}

.probe-dot {
  stroke: var(--surface-solid);
  stroke-width: 2;
  filter: drop-shadow(0 0 12px rgba(66, 230, 164, 0.38));
}

.probe-dot.loss {
  fill: #fb7185;
}

.probe-dot.accuracy {
  fill: #42e6a4;
}

.svg-tooltip rect {
  fill: rgba(var(--glass-bg-rgb), 0.82);
  stroke: rgba(var(--primary-rgb), 0.22);
  backdrop-filter: blur(14px);
}

.svg-tooltip text {
  fill: var(--text-primary);
  font-size: 10px;
  font-weight: var(--font-weight-label);
}

.svg-tooltip.compact text {
  font-size: 9px;
}

.signal-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.signal-card {
  min-height: 104px;
  padding: 14px;
}

.signal-card strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: var(--font-weight-title);
}

.sparkline {
  height: 34px;
  display: flex;
  align-items: end;
  gap: 3px;
  margin-top: 10px;
}

.sparkline i {
  flex: 1;
  min-width: 3px;
  border-radius: 999px 999px 2px 2px;
  opacity: 0.78;
  transition: opacity 120ms ease, transform 120ms ease, box-shadow 120ms ease;
  transform-origin: center bottom;
}

.sparkline i.active,
.node-bars i.active {
  opacity: 1;
  transform: scaleY(1.14);
  box-shadow: 0 0 14px rgba(66, 230, 164, 0.26);
}

.dom-tooltip {
  position: absolute;
  z-index: 3;
  min-width: 76px;
  padding: 7px 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.86);
  color: var(--text-primary);
  box-shadow: var(--shadow-soft);
  transform: translate(-50%, -110%);
  pointer-events: none;
  backdrop-filter: blur(14px);
}

.dom-tooltip strong,
.dom-tooltip span {
  display: block;
}

.dom-tooltip strong {
  font-size: 13px;
  font-weight: var(--font-weight-title);
}

.dom-tooltip span {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 9px;
  font-weight: var(--font-weight-label);
}

.component-preview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0 0;
}

.component-node {
  position: relative;
  min-height: 118px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, rgba(66, 230, 164, 0.08), transparent 52%),
    color-mix(in srgb, var(--surface-1) 78%, transparent);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.component-node::after {
  content: "";
  position: absolute;
  inset: auto 14px 14px auto;
  width: 36px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #42e6a4);
}

.component-node span {
  display: block;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.component-node strong {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.node-bars {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 16px;
  height: 34px;
  display: flex;
  align-items: end;
  gap: 4px;
}

.node-bars i {
  flex: 1;
  min-width: 5px;
  border-radius: 2px 2px 999px 999px;
  background: linear-gradient(180deg, #42e6a4, #67e8f9);
  box-shadow: 0 0 12px rgba(66, 230, 164, 0.2);
  opacity: 0.86;
  transition: opacity 120ms ease, transform 120ms ease, box-shadow 120ms ease;
  transform-origin: center bottom;
}

.viz-modules {
  margin-top: 0;
}

.section-caption {
  position: relative;
  z-index: 1;
  padding-bottom: 12px;
  margin: 0 0 16px;
  border-bottom: 1px solid var(--border-color);
}

.module-workbench-head {
  align-items: center;
}

.module-workbench-head p {
  max-width: 760px;
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: var(--font-weight-body);
  line-height: 1.65;
}

.analysis-mode-switch.compact {
  min-width: min(320px, 100%);
}

.module-lab {
  display: grid;
  grid-template-columns: minmax(300px, 0.82fr) minmax(0, 1.55fr);
  gap: 16px;
  align-items: start;
  min-height: 0;
  overflow: visible;
}

.module-preview-panel {
  position: relative;
  min-height: 120px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 22%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--active-accent) 12%, transparent), transparent 54%),
    color-mix(in srgb, var(--surface-1) 82%, transparent);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.module-preview-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--active-accent) 14%, transparent) 1px, transparent 1px),
    linear-gradient(180deg, color-mix(in srgb, var(--active-accent) 12%, transparent) 1px, transparent 1px);
  background-size: 34px 34px;
  opacity: 0.26;
}

.module-preview-copy,
.module-preview-meta,
.module-preview-visual {
  position: relative;
  z-index: 1;
}

.module-preview-copy span {
  display: block;
  color: color-mix(in srgb, var(--active-accent) 72%, var(--text-muted));
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.module-preview-copy strong {
  display: block;
  margin-top: 7px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: var(--font-weight-title);
  line-height: 1.08;
}

.module-preview-copy p {
  max-width: 34ch;
  margin: 10px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.72;
}

.module-preview-visual {
  height: 292px;
  margin-top: 18px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 24%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 18% 0%, color-mix(in srgb, var(--active-accent) 14%, transparent), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.035), transparent),
    rgba(var(--glass-bg-rgb), 0.18);
  overflow: hidden;
  transition: border-color 180ms ease, background 220ms ease, opacity 180ms ease, transform 180ms ease;
}

.module-preview-visual.is-switching {
  opacity: 0.55;
  transform: translateY(4px) scale(0.992);
}

.module-art-grid {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--active-accent) 12%, transparent) 1px, transparent 1px),
    linear-gradient(180deg, rgba(148, 163, 184, 0.13) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: radial-gradient(circle at 50% 48%, black, transparent 78%);
}

.module-art-svg {
  position: absolute;
  inset: 22px 14px 42px;
  width: calc(100% - 28px);
  height: calc(100% - 64px);
  overflow: hidden;
}

.module-art-area {
  fill: color-mix(in srgb, var(--active-accent) 18%, transparent);
}

.module-art-path {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}

.module-art-path.primary {
  stroke: var(--active-accent);
  stroke-width: 3.2;
  filter: drop-shadow(0 0 10px color-mix(in srgb, var(--active-accent) 46%, transparent));
  transition: d 180ms ease;
}

.module-art-path.secondary {
  stroke: rgba(148, 163, 184, 0.55);
  stroke-width: 1.6;
  stroke-dasharray: 6 8;
}

.module-art-bar {
  fill: var(--active-accent);
  opacity: 0.78;
  filter: drop-shadow(0 8px 16px color-mix(in srgb, var(--active-accent) 18%, transparent));
  transition: opacity 140ms ease, transform 140ms ease;
  transform-box: fill-box;
  transform-origin: center bottom;
}

.module-art-bar.active {
  opacity: 1;
  transform: scaleY(1.08);
}

.module-art-node {
  fill: var(--active-accent);
  stroke: color-mix(in srgb, var(--surface-solid) 82%, transparent);
  stroke-width: 2;
  filter: drop-shadow(0 0 10px color-mix(in srgb, var(--active-accent) 38%, transparent));
  transition: r 140ms ease, opacity 140ms ease, transform 140ms ease;
}

.module-art-node.active {
  opacity: 1;
  transform-box: fill-box;
  transform-origin: center;
  transform: scale(1.35);
}

.module-art-edge {
  stroke: color-mix(in srgb, var(--active-accent) 42%, rgba(148, 163, 184, 0.42));
  stroke-width: 1.2;
  vector-effect: non-scaling-stroke;
}

.module-svg-probe .probe-line {
  stroke: color-mix(in srgb, var(--active-accent) 72%, transparent);
}

.module-readout {
  position: absolute;
  z-index: 4;
  min-width: 96px;
  padding: 8px 9px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 26%, transparent);
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.86);
  box-shadow: 0 16px 38px color-mix(in srgb, var(--active-accent) 12%, transparent);
  transform: translate(-50%, -118%);
  pointer-events: none;
  backdrop-filter: blur(14px);
}

.module-readout strong,
.module-readout span {
  display: block;
}

.module-readout strong {
  color: var(--active-accent);
  font-size: 15px;
  font-weight: var(--font-weight-title);
}

.module-readout span {
  margin-top: 3px;
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: var(--font-weight-label);
}

.module-art-code {
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 12px;
  display: grid;
  gap: 5px;
}

.module-art-code span {
  width: max-content;
  max-width: 100%;
  padding: 4px 7px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 18%, transparent);
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.28);
  color: color-mix(in srgb, var(--active-accent) 76%, var(--text-secondary));
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: var(--font-weight-label);
  white-space: nowrap;
}

.module-empty-state {
  inset: 54px 18px 58px;
}

.module-preview-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.module-preview-meta span {
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: color-mix(in srgb, var(--surface-2) 82%, transparent);
}

.module-preview-meta strong,
.module-preview-meta em {
  display: block;
}

.module-preview-meta strong {
  color: var(--active-accent);
  font-size: 18px;
  font-weight: var(--font-weight-title);
}

.module-preview-meta em {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.module-analysis-strip {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  --spotlight-x: 50%;
  --spotlight-y: 50%;
  --spotlight-color: color-mix(in srgb, var(--active-accent) 18%, transparent);
  z-index: 1;
  margin-top: 14px;
  padding: 13px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 18%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 14% 0%, color-mix(in srgb, var(--active-accent) 10%, transparent), transparent 42%),
    color-mix(in srgb, var(--surface-2) 72%, transparent);
}

.module-analysis-strip .matrix-control-head {
  margin-bottom: 10px;
}

.analysis-smart-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.matrix-run-picker {
  margin-bottom: 16px;
}

.compact-runs {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.embedded-results {
  margin-top: 18px;
}

.module-grid-panel {
  min-width: 0;
  min-height: 0;
  overflow: visible;
  padding-right: 0;
}

.module-card {
  position: relative;
  --spotlight-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 18%, transparent);
  min-height: 200px;
  padding: 18px 18px 62px 18px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
  outline: none;
}

.module-card.active {
  border-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 34%, var(--border-color)) !important;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--module-accent, var(--active-accent)) 10%, transparent), transparent 64%),
    var(--surface-2) !important;
  box-shadow: 0 18px 46px color-mix(in srgb, var(--module-accent, var(--active-accent)) 12%, transparent) !important;
}

.module-card.selected {
  outline: 1px solid color-mix(in srgb, var(--module-accent, var(--active-accent)) 42%, transparent);
  outline-offset: 2px;
}

.module-card.picked {
  border-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 68%, #020617) !important;
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--module-accent, var(--active-accent)) 34%, transparent),
    0 22px 60px color-mix(in srgb, var(--module-accent, var(--active-accent)) 20%, transparent) !important;
}

.module-card.analyzable::after {
  content: "";
  position: absolute;
  top: 12px;
  right: 12px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--module-accent, var(--active-accent)) 74%, white);
  box-shadow: 0 0 18px color-mix(in srgb, var(--module-accent, var(--active-accent)) 48%, transparent);
}

.module-card.diving {
  transform: translateY(2px) scale(0.985);
}

.module-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  color: var(--text-muted);
  font-size: 9px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.module-select-toggle {
  position: absolute;
  right: 10px;
  bottom: 10px;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  padding: 5px 10px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.8);
  color: var(--text-muted);
  cursor: pointer;
  font-size: 11px;
  font-weight: 550;
  line-height: 1.3;
  text-align: left;
  text-transform: none;
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  transition: opacity 160ms ease, border-color 160ms ease, background 160ms ease, color 160ms ease, box-shadow 160ms ease;
}

.module-select-toggle span {
  color: var(--text-primary);
}
.module-select-toggle b {
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 550;
}

.module-select-toggle.analyzable b {
  color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 72%, white);
}

.module-select-toggle.picked {
  border-color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 72%, #020617);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--module-accent, var(--active-accent)) 28%, transparent), rgba(0, 0, 0, 0.2)),
    rgba(15, 23, 42, 0.86);
  color: color-mix(in srgb, var(--module-accent, var(--active-accent)) 86%, white);
  opacity: 1;
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--module-accent, var(--active-accent)) 20%, transparent),
    0 14px 32px color-mix(in srgb, var(--module-accent, var(--active-accent)) 18%, transparent);
}

.module-select-toggle:hover {
  opacity: 1;
  transform: translateY(-2px);
}

.module-icon {
  margin-bottom: 12px;
  color: var(--module-accent, var(--active-accent));
}

.module-card h3 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: var(--font-weight-title);
}

.module-card p {
  margin: 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.65;
}

.module-mini-preview {
  position: absolute;
  left: 14px;
  bottom: 17px;
  width: calc(100% - 124px);
  max-width: 92px;
  height: 32px;
  display: flex;
  align-items: end;
  gap: 3px;
  opacity: 0.74;
}

.module-mini-preview i {
  flex: 1;
  min-width: 4px;
  border-radius: 999px 999px 2px 2px;
  background: var(--module-accent, var(--active-accent));
  transition: opacity 120ms ease, transform 120ms ease, box-shadow 120ms ease;
  transform-origin: center bottom;
}

.module-mini-preview i.active {
  opacity: 1;
  transform: scaleY(1.18);
  box-shadow: 0 0 12px color-mix(in srgb, var(--module-accent, var(--active-accent)) 34%, transparent);
}

.mini-tooltip {
  min-width: 96px;
}

.module-detail-panel {
  position: relative;
  margin-top: 18px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 28%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 16% 0%, color-mix(in srgb, var(--active-accent) 12%, transparent), transparent 40%),
    linear-gradient(180deg, color-mix(in srgb, var(--active-accent) 8%, transparent), transparent 44%),
    color-mix(in srgb, var(--surface-1) 78%, transparent);
  box-shadow: 0 24px 70px color-mix(in srgb, var(--active-accent) 12%, transparent);
  overflow: hidden;
}

.module-detail-panel::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--active-accent) 12%, transparent) 1px, transparent 1px),
    linear-gradient(180deg, rgba(148, 163, 184, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, black, transparent 88%);
}

.module-detail-panel::after {
  content: "";
  position: absolute;
  left: 24px;
  right: 24px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--active-accent), transparent);
}

.detail-head,
.detail-body {
  position: relative;
  z-index: 1;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.detail-head span,
.detail-readout span,
.detail-table span {
  display: block;
  color: color-mix(in srgb, var(--active-accent) 64%, var(--text-muted));
  font-size: 10px;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.detail-head strong {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: var(--font-weight-title);
}

.detail-head p {
  max-width: 56ch;
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(90px, 1fr));
  gap: 10px;
  min-width: 220px;
}

.detail-stats span,
.detail-readout,
.detail-lines,
.detail-table div {
  border: 1px solid color-mix(in srgb, var(--active-accent) 18%, var(--border-color));
  border-radius: var(--radius-md);
  background: rgba(var(--glass-bg-rgb), 0.28);
}

.detail-stats span {
  padding: 10px 12px;
}

.detail-stats strong,
.detail-stats em {
  display: block;
}

.detail-stats strong {
  color: var(--active-accent);
  font-size: 18px;
  font-weight: var(--font-weight-title);
}

.detail-stats em {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 10px;
  font-style: normal;
  font-weight: var(--font-weight-title);
  text-transform: uppercase;
}

.detail-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 0.32fr);
  gap: 16px;
}

.detail-chart {
  position: relative;
  min-height: 380px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 22%, var(--border-color));
  border-radius: var(--radius-lg);
  background: rgba(var(--glass-bg-rgb), 0.18);
  overflow: hidden;
}

.detail-svg {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 380px;
  overflow: hidden;
}

.detail-empty-state {
  inset: 28px;
}

.detail-plot-bg {
  fill: rgba(255, 255, 255, 0.026);
  stroke: rgba(148, 163, 184, 0.16);
}

.detail-grid line {
  stroke: rgba(148, 163, 184, 0.22);
  vector-effect: non-scaling-stroke;
}

.detail-area {
  fill: url(#detailFill);
}

.detail-path {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}

.detail-path.primary {
  stroke: var(--active-accent);
  stroke-width: 3.2;
  filter: drop-shadow(0 0 12px color-mix(in srgb, var(--active-accent) 34%, transparent));
  animation: draw-curve 620ms cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.detail-path.secondary {
  stroke: rgba(148, 163, 184, 0.58);
  stroke-width: 1.5;
  stroke-dasharray: 7 9;
}

.detail-bar {
  fill: var(--active-accent);
  opacity: 0.76;
  filter: drop-shadow(0 12px 18px color-mix(in srgb, var(--active-accent) 16%, transparent));
  transform-box: fill-box;
  transform-origin: center bottom;
  transition: opacity 140ms ease, transform 140ms ease;
}

.detail-bar.active {
  opacity: 1;
  transform: scaleY(1.08);
}

.detail-node {
  fill: var(--active-accent);
  stroke: color-mix(in srgb, var(--surface-solid) 82%, transparent);
  stroke-width: 2;
  filter: drop-shadow(0 0 10px color-mix(in srgb, var(--active-accent) 34%, transparent));
  transform-box: fill-box;
  transform-origin: center;
  transition: transform 140ms ease, opacity 140ms ease;
}

.detail-node.active {
  transform: scale(1.42);
}

.detail-edge {
  stroke: color-mix(in srgb, var(--active-accent) 42%, rgba(148, 163, 184, 0.42));
  stroke-width: 1.2;
  vector-effect: non-scaling-stroke;
}

.detail-inspector {
  display: grid;
  align-content: start;
  gap: 12px;
}

.detail-readout {
  padding: 14px;
}

.detail-readout strong {
  display: block;
  margin-top: 9px;
  color: var(--active-accent);
  font-size: 20px;
  font-weight: var(--font-weight-title);
}

.detail-readout em {
  display: block;
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 11px;
  font-style: normal;
  font-weight: var(--font-weight-label);
}

.detail-lines {
  display: grid;
  gap: 6px;
  padding: 12px;
}

.detail-lines span {
  width: max-content;
  max-width: 100%;
  padding: 5px 8px;
  border: 1px solid color-mix(in srgb, var(--active-accent) 18%, transparent);
  border-radius: var(--radius-sm);
  background: rgba(var(--glass-bg-rgb), 0.28);
  color: color-mix(in srgb, var(--active-accent) 76%, var(--text-secondary));
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 10px;
  font-weight: var(--font-weight-label);
  white-space: nowrap;
}

.detail-table {
  display: grid;
  gap: 8px;
}

.detail-table div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
}

.detail-table strong {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: var(--font-weight-title);
}

.detail-panel-enter-active,
.detail-panel-leave-active {
  transition: opacity 220ms ease, transform 220ms ease, filter 220ms ease;
}

.detail-panel-enter-from,
.detail-panel-leave-to {
  opacity: 0;
  transform: translateY(18px) scale(0.985);
  filter: blur(8px);
}

@media (max-width: 980px) {
  .band-caption {
    align-items: flex-start;
  }

  .matrix-controls,
  .matrix-result-grid {
    grid-template-columns: 1fr;
  }

  .view-save-grid {
    grid-template-columns: 1fr;
  }

  .one-many-module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .preview-stage {
    grid-template-columns: 1fr;
  }

  .component-preview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .module-lab {
    grid-template-columns: 1fr;
    height: auto;
    overflow: visible;
  }

  .module-preview-panel {
    position: relative;
    top: auto;
    min-height: auto;
    max-height: none;
  }

  .module-preview-visual {
    height: 260px;
  }
}

@media (max-width: 640px) {
  .vizhub-page {
    padding: 16px;
  }

  .band-caption,
  .matrix-action-stack {
    display: grid;
    justify-items: stretch;
    min-width: 0;
  }

  .analysis-mode-switch {
    min-width: 0;
  }

  .matrix-action-stack em {
    text-align: left;
  }

  .matrix-module-grid,
  .one-many-module-grid {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .signal-strip,
  .component-preview {
    grid-template-columns: 1fr;
  }
}

@keyframes draw-curve {
  from {
    stroke-dasharray: 1000;
    stroke-dashoffset: 1000;
  }
  to {
    stroke-dasharray: 1000;
    stroke-dashoffset: 0;
  }
}

</style>
