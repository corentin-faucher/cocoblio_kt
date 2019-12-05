/*-- Fragment shader -----------------*/
/*-- Corentin Faucher, 3 fev 2019 --*/
precision mediump float;
varying vec2  uvOut;
varying vec4  colorOut;

uniform sampler2D tex;

void main()
{
    gl_FragColor = texture2D(tex, uvOut) * colorOut;
}


