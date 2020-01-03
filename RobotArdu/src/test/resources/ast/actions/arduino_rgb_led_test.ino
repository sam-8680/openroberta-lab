// This file is automatically generated by the Open Roberta Lab.

#include <Arduino.h>
#include <math.h>
#include <RobertaFunctions/NEPODefs.h>


unsigned int ___item;
unsigned int ___item2;
unsigned int ___item3;
int _led_red_R = 5;
int _led_green_R = 6;
int _led_blue_R = 3;
void setup()
{
    pinMode(_led_red_R, OUTPUT);
    pinMode(_led_green_R, OUTPUT);
    pinMode(_led_blue_R, OUTPUT);
    ___item = RGB(0xFF, 0xFF, 0xFF);
    ___item2 = ___item;
    ___item3 = RGB(120, 120, 120);
}

void loop()
{
    analogWrite(_led_red_R, 204);
    analogWrite(_led_green_R, 0);
    analogWrite(_led_blue_R, 0);
    
    analogWrite(_led_red_R, RCHANNEL(___item));
    analogWrite(_led_green_R, GCHANNEL(___item));
    analogWrite(_led_blue_R, BCHANNEL(___item));
    
    analogWrite(_led_red_R, 120);
    analogWrite(_led_green_R, 120);
    analogWrite(_led_blue_R, 120);
    
    analogWrite(_led_red_R, 0);
    analogWrite(_led_green_R, 0);
    analogWrite(_led_blue_R, 0);
    
}
