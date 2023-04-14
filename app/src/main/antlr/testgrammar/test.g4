grammar test;
@header {
    package testgrammar;
}
str   : 'hello' ID;
ID  : [a-z]+ ;
WS: [ \n\t\r]+ -> skip;
