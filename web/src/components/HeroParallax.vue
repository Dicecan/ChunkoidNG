<script setup>

import { ref, onMounted, onBeforeUnmount } from 'vue'
import { t } from '../i18n'

const card = ref(null)
const layerBg = ref(null)
const layerChar = ref(null)
const layerGrass = ref(null)
const layerGlare = ref(null)

let raf = 0
let lastFrameTime = 0

let targetShiftX = 0, targetShiftY = 0
let curShiftX = 0, curShiftY = 0
let targetGlareX = 50, targetGlareY = 50
let curGlareX = 50, curGlareY = 50

let swayPhase = 0
let autoSway = true
let pointerActive = false
let pointerTimer = 0
let gyroActive = false
let gyroBase = null
let gyroHandler = null
let reduceMotion = false
let motionQuery = null

let compact = false
let sizeQuery = null

const K_BG = -0.55
const K_CHAR = 0.40
const K_GRASS = 1.15

const MAX_SHIFT = () => (compact ? 12 : 26)
const GLARE_RANGE = () => (compact ? 20 : 38)

function dampFactor(k, dt) {
  return 1 - Math.pow(1 - k, dt * 60)
}

function render(now) {
  const dt = lastFrameTime ? Math.min((now - lastFrameTime) / (1000 / 60), 4) : 1
  lastFrameTime = now

  if (autoSway && !reduceMotion && !pointerActive && !gyroActive) {
    swayPhase += 0.022 * dt
    const amp = compact ? 0.4 : 1
    targetShiftX += (Math.sin(swayPhase * 0.85) * 7.5 * amp - targetShiftX) * 0.04 * dt
    targetShiftY += (Math.cos(swayPhase * 0.65) * 5.0 * amp - targetShiftY) * 0.04 * dt
  }

  const lerp = dampFactor(0.082, dt)
  const glareLerp = dampFactor(0.1, dt)
  curShiftX += (targetShiftX - curShiftX) * lerp
  curShiftY += (targetShiftY - curShiftY) * lerp
  curGlareX += (targetGlareX - curGlareX) * glareLerp
  curGlareY += (targetGlareY - curGlareY) * glareLerp

  if (layerBg.value) {
    layerBg.value.style.transform =
      `translate3d(${(curShiftX * K_BG).toFixed(1)}px, ${(curShiftY * K_BG).toFixed(1)}px, -30px) scale(1.16)`
  }
  if (layerChar.value) {
    layerChar.value.style.transform =
      `translate3d(${(curShiftX * K_CHAR).toFixed(1)}px, ${(curShiftY * K_CHAR).toFixed(1)}px, 14px) scale(1.1)`
  }
  if (layerGrass.value) {
    layerGrass.value.style.transform =
      `translate3d(${(curShiftX * K_GRASS).toFixed(1)}px, ${(curShiftY * K_GRASS).toFixed(1)}px, 46px) scale(1.2)`
  }
  if (layerGlare.value) {
    layerGlare.value.style.background =
      `radial-gradient(circle at ${curGlareX.toFixed(1)}% ${curGlareY.toFixed(1)}%, rgba(255,255,255,0.3) 0%, transparent 62%)`
  }

  raf = requestAnimationFrame(render)
}

function movePointerTo(clientX, clientY) {
  const el = card.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2

  const normX = (clientX - cx) / (window.innerWidth / 2)
  const normY = (clientY - cy) / (window.innerHeight / 2)
  const clX = Math.max(-1.2, Math.min(1.2, normX))
  const clY = Math.max(-1.2, Math.min(1.2, normY))
  const s = MAX_SHIFT()
  targetShiftX = clX * s
  targetShiftY = clY * s * 0.75
  const g = GLARE_RANGE()
  targetGlareX = 50 - clX * g
  targetGlareY = 50 - clY * g
}

function handleMouseMove(e) {
  if (reduceMotion) return
  pointerActive = true
  movePointerTo(e.clientX, e.clientY)
  clearTimeout(pointerTimer)
  pointerTimer = setTimeout(() => { pointerActive = false }, 1200)
}

function handleTouchMove(e) {
  if (reduceMotion || !e.touches || !e.touches.length) return
  pointerActive = true
  movePointerTo(e.touches[0].clientX, e.touches[0].clientY)
  clearTimeout(pointerTimer)
  pointerTimer = setTimeout(() => { pointerActive = false }, 1200)
}

function handleOrientation(e) {
  if (e.beta === null || e.gamma === null || reduceMotion || pointerActive) return
  if (gyroBase === null) gyroBase = { beta: e.beta, gamma: e.gamma }
  let dBeta = e.beta - gyroBase.beta
  let dGamma = e.gamma - gyroBase.gamma
  const orientation = window.orientation || (screen.orientation && screen.orientation.angle) || 0
  if (orientation === 90) { const tmp = dBeta; dBeta = -dGamma; dGamma = tmp }
  else if (orientation === -90) { const tmp = dBeta; dBeta = dGamma; dGamma = -tmp }
  dBeta = Math.max(-26, Math.min(26, dBeta))
  dGamma = Math.max(-30, Math.min(30, dGamma))
  gyroActive = true
  const s = MAX_SHIFT()
  targetShiftX = (dGamma / 30) * s
  targetShiftY = (dBeta / 26) * s * 0.75
  const g = GLARE_RANGE()
  targetGlareX = 50 - (dGamma / 30) * g
  targetGlareY = 50 - (dBeta / 26) * g
}

function applyMotionPreference(matches) {
  reduceMotion = matches
  autoSway = !matches
  if (matches) {
    targetShiftX = targetShiftY = 0
    targetGlareX = targetGlareY = 50
    gyroActive = false
  }
}

function applySizePreference(matches) {
  compact = matches
  targetShiftX = 0
  targetShiftY = 0
  curShiftX = 0
  curShiftY = 0
}

function attachInputListeners() {
  window.addEventListener('mousemove', handleMouseMove, { passive: true })
  window.addEventListener('touchmove', handleTouchMove, { passive: true })
  if (window.DeviceOrientationEvent && typeof DeviceOrientationEvent.requestPermission !== 'function') {
    gyroHandler = handleOrientation
    window.addEventListener('deviceorientation', gyroHandler, true)
  }
}

function detachInputListeners() {
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('touchmove', handleTouchMove)
  if (gyroHandler) {
    window.removeEventListener('deviceorientation', gyroHandler, true)
    gyroHandler = null
  }
}

function onMotionPreferenceChange(e) {
  applyMotionPreference(e.matches)
  if (e.matches) detachInputListeners()
  else attachInputListeners()
}

function onSizePreferenceChange(e) {
  applySizePreference(e.matches)
}

onMounted(() => {
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  applyMotionPreference(motionQuery.matches)
  if (motionQuery.addEventListener) motionQuery.addEventListener('change', onMotionPreferenceChange)
  else if (motionQuery.addListener) motionQuery.addListener(onMotionPreferenceChange)

  sizeQuery = window.matchMedia('(max-width: 900px)')
  applySizePreference(sizeQuery.matches)
  if (sizeQuery.addEventListener) sizeQuery.addEventListener('change', onSizePreferenceChange)
  else if (sizeQuery.addListener) sizeQuery.addListener(onSizePreferenceChange)

  raf = requestAnimationFrame(render)
  if (!reduceMotion) attachInputListeners()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  clearTimeout(pointerTimer)
  detachInputListeners()
  if (motionQuery) {
    if (motionQuery.removeEventListener) motionQuery.removeEventListener('change', onMotionPreferenceChange)
    else if (motionQuery.removeListener) motionQuery.removeListener(onMotionPreferenceChange)
  }
  if (sizeQuery) {
    if (sizeQuery.removeEventListener) sizeQuery.removeEventListener('change', onSizePreferenceChange)
    else if (sizeQuery.removeListener) sizeQuery.removeListener(onSizePreferenceChange)
  }
})
</script>

<template>
  <div class="parallax-bg">
    <div ref="card" class="tilt">
      <div ref="layerBg" class="layer layer-bg">
        <img src="/hero/layer1-bg.webp" :alt="t('home.heroAlt')" width="1376" height="768" />
      </div>
      <div ref="layerChar" class="layer layer-char">
        <img src="/hero/layer2-char.webp" alt="" width="1600" height="900" aria-hidden="true" />
      </div>
      <div ref="layerGrass" class="layer layer-grass">
        <img src="/hero/layer3-grass.webp" alt="" width="1600" height="900" aria-hidden="true" />
      </div>
      <div ref="layerGlare" class="glare"></div>
    </div>
  </div>
</template>

<style scoped>
.parallax-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  perspective: 1000px;
  background: #0a0f0d;
}

.tilt {
  position: absolute;
  inset: -52px;
  transform-style: preserve-3d;
}

.layer {
  position: absolute;
  inset: 0;
  transform-style: preserve-3d;
  will-change: transform;
  pointer-events: none;
}

.layer img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 48%;
  pointer-events: none;
}

.layer-bg { z-index: 1; transform: translate3d(0, 0, -30px) scale(1.16); }
.layer-char { z-index: 2; transform: translate3d(0, 0, 14px) scale(1.1); }
.layer-grass { z-index: 3; transform: translate3d(0, 0, 46px) scale(1.2); }

.glare {
  position: absolute;
  inset: 0;
  z-index: 5;
  pointer-events: none;
  mix-blend-mode: screen;
  opacity: 0.18;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.3) 0%, transparent 62%);
}

@media (max-width: 900px) {
  .tilt { inset: -30px; }
  .glare { opacity: 0.13; }
}

@media (prefers-reduced-motion: reduce) {
  .layer { will-change: auto; }
}
</style>