/*-- Vertex shader -----------------*/
/*-- Corentin Faucher, 2 fev 2019 --*/

// Vertex attributes
attribute vec4 position;
attribute vec3 normal;
attribute vec2 uv;

// Per frame constants
uniform mat4 projection;
uniform float time;

// Per texture constants
uniform vec2 texWH;
uniform vec2 texMN;

// Per object constants
uniform mat4  model;
uniform vec2  texIJ;
uniform vec4  color;
uniform float emph;
uniform int   flags;

// Retourné au fragment shader
varying vec2 uvOut;
varying vec4 colorOut;

void main() {
    colorOut = vec4(color.xyz, 1.) * color.a;

    vec4 posTmp = position;
    // Déformation d'emphase et oscillation
    if(emph > 0.) {
        posTmp = posTmp * (1. + emph * vec4(0.18+0.10*sin(time*6.), 0.18+0.10*sin(time*6.+1.), 0., 0.) );
    }

    // Coord. uv en fonction de la tile.
    uvOut = (uv * (texWH - texMN) + texIJ * texWH) / (texMN*(texWH - 1.));

    gl_Position = projection * model * posTmp;
    //gl_Position = projection * model * posTmp;
}

/*
    vUVrel = (vVerUV-0.5)*2.; // Espace de travail entre -1 et 1 pour jouer avec les textures...
    // Lumière et normales
    vec4 lightDirection = vec4(0,0,1,0);
    vec4 normalTmp = vec4(normal, 0.);
    normalTmp = normalize(model * normalTmp);

    colorOut = vec4(color.xyz * max(0.2,abs(dot(lightDirection,normalTmp))), color.a);
*/