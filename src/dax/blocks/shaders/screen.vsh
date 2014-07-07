#version 120

//======================//
//= EDITABLE VARIABLES =//
//======================//

#define ENABLE_ACID

//=============================//
//= END OF EDITABLE VARIABLES =//
//=============================//

attribute float blockid;

uniform float lighting;

varying vec4 color;
varying vec4 vposition;
varying float vdist;
varying float blockidinterp;

uniform float time;

float rand(vec3 co){
    return fract(sin(dot(co.xz, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
	blockidinterp = blockid;
	
	vec3 vertexPosition = (gl_ModelViewMatrix * gl_Vertex).xyz;
	vec3 lightDirection = normalize(gl_LightSource[0].position.xyz);
	vec3 surfaceNormal  = (gl_NormalMatrix * gl_Normal).xyz;

	float diffuseLightIntensity = max(0, dot(surfaceNormal, lightDirection));
	
	if (lighting < 1.0) {
		color.rgba = gl_Color.rgba;
	} else {
		
		float lightmult = min((diffuseLightIntensity + (gl_LightModel.ambient.rgb.x * gl_LightModel.ambient.rgb.y * gl_LightModel.ambient.rgb.z)), 1.0);
		
		color.rgb = lightmult * gl_Color.rgb;
		color.a = gl_Color.a;
	}	

    vec4 vertex = gl_Vertex;
    
    if (blockid == 12) {
    	float distanceSquared = vertex.x * vertex.x + vertex.z * vertex.z;
    	vertex.y -= sin(time*2.0+sqrt(distanceSquared)*1.5)*0.125;
    }
    
    #ifdef ENABLE_ACID
    
    vertex.x += cos(vertex.x+time*0.5)*0.2;
    vertex.y += sin(vertex.x+time*2)*0.22;  
    vertex.z += sin(vertex.x+time)*0.25;  
    
    #endif   
    
    vec4 position = gl_ModelViewMatrix * vertex;  

    #ifdef ENABLE_ACID
    
    // Not a cloud
    if (gl_Color.a != 0.7) {
                float distanceSquared = position.x * position.x + position.z * position.z;
                position.y += 5*sin(distanceSquared*sin(float(time)/6.0)/1500);
                float y = position.y;
                float x = position.x;
                float om = sin(distanceSquared*sin(float(time)/10.0)/6000) * sin(float(time)/8.0);
                position.y = x*sin(om)+y*cos(om);
                position.x = x*cos(om)-y*sin(om);
    }        
    
    #endif   
    
    gl_Position = gl_ProjectionMatrix * position;
    vposition = position;
    
    vdist = distance(vposition.xyz, vec3(0.0, 0.0, 0.0));
    
    //gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;
}