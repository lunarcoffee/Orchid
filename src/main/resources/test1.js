function print(msg) {
    console.log(msg);
}

function getNumArr() {
    var number = 1.0;
    var arr = [number, (2.0) & (1.0), 4.0];
    {
        arr = [1.0, number, 3.0];
        var a = 1.0;
        arr = [a, number, 3.0];
    }
    return arr;
}

function getNumber(arg) {
    return (arg) + (Math.pow(53942.233, 0.1));
}

var i;
var j = getNumArr();
var s = "String";
print("Orchid");
print(s);
s = "String again";
print(s);
var m = ((getNumber(-1239.2)) * (Math.pow(2.0, Math.pow(5.0, 3.0)))) + (2.0);
var k = (3.0) + (((m) * (2.0)) / (Math.pow((1.0) - (getNumber(3.0)), Math.pow(-2.0, 3.0))));
var b = false;
if (b) print("hi"); else if (true) print("bye"); else {
    var a = 1.0;
    print(a);
}