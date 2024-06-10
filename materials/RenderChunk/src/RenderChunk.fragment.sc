$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra, vPos, RainTime, wPos, v_FOG_COLOR, v_zenithCol, v_horizonCol, v_horizonEdgeCol

#include <bgfx_shader.sh>
#include <newb/main.sh>

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_SeasonsTexture, 1);
SAMPLER2D(s_LightMapTexture, 2);

void main() {
  vec4 diffuse;
  vec4 color;
  float time = RainTime.y;
  float rain = RainTime.x;
  vec3 vDir = normalize(wPos.xyz);
  
  vec3 zenithCol;
  vec3 horizonCol;
  vec3 horizonEdgeCol;
    vec3 fs = getSkyFactors(v_FOG_COLOR.rgb);
    zenithCol = getZenithCol(rain, v_FOG_COLOR.rgb, fs);
    horizonCol = getHorizonCol(rain, v_FOG_COLOR.rgb, fs);
    horizonEdgeCol = getHorizonEdgeCol(horizonCol, rain, v_FOG_COLOR.rgb);
  

#if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY)
  diffuse = vec4(1.0,1.0,1.0,1.0);
  color = vec4(1.0,1.0,1.0,1.0);
#else
  diffuse = texture2D(s_MatTexture, v_texcoord0);

#ifdef ALPHA_TEST
  if (diffuse.a < 0.6) {
    discard;
  }
#endif

#if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
  diffuse.rgb *= mix(vec3(1.0,1.0,1.0), texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0, v_color1.z);
#endif
  color = v_color0;
#endif

  diffuse.rgb *= diffuse.rgb;

  vec3 lightTint = texture2D(s_LightMapTexture, v_lightmapUV).rgb;
  lightTint = mix(lightTint.bbb, lightTint*lightTint, 0.35 + 0.65*v_lightmapUV.y*v_lightmapUV.y*v_lightmapUV.y);

  color.rgb *= lightTint;

  vec3 glow = nlGlow(s_MatTexture, v_texcoord0, diffuse, v_extra.a);

vec4 refl = vec4(0.0,0.0,0.0,0.0);
#ifdef TRANSPARENT
  if (v_extra.b > 0.9) {
    //diffuse.rgb += wReflection(vDir, wPos.xyz, rain, time, v_FOG_COLOR, zenithCol, horizonCol, horizonEdgeCol);
    diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
    
    diffuse.a = color.a;
    }else{
  }
#else
  diffuse.a = 1.0;
#endif

  diffuse.rgb *= color.rgb;
  diffuse.rgb += glow;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;
    diffuse.rgb += wReflection(vDir, wPos.xyz, rain, time, v_FOG_COLOR, zenithCol, horizonCol, horizonEdgeCol);
  } else if (v_refl.a > 0.0) {
    // reflective effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb *= 1.0 - 0.6*mask;
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
