# Orchid
Experimental statically typed language that compiles to JavaScript. Here is some example source code (with Swift source highlighting) that compiles under the current version of Orchid:
```swift
var i: Number;
var j = getNumArr();
var s = "String";

print("Orchid");
print(s);
s = "String again";
print(s);

var m = getNumber(-1239.2) * 2 ** 5 ** 3 + 2;
var k = 3 + m * 2 / (1 - getNumber(3)) ** -2 ** 3;

when (s + "!") {
    "Number" -> print("number");
    "String" -> {
        var a = 1;
        print(a);
    }
    "hello", "String again!", "a" -> print("string!");
    else -> print("error!");
}

for (var r = 0; r <= 10; r += 1;) {
    print(r);
    r += 1;
}

foreach (var d; 3=.10;)
    print(d);

# This is a comment!
func print(msg: Any): Void {
    extern func console.log(Any): Void;
    console.log(msg);
}

func getNumArr(): Array<Number> {
    var number = 1;
    var arr = []Number{number, 2 & 1, 4 + +"5"};

    {
        number >|= 3;
        arr = number=.number + 3;
        var a = 1;
        arr = []Number{a, number, +"339" >| 3};
    }

    when (number + 1) {
        in arr -> print("in arr!");
        in 1=.3 -> print("in array literal!");
    }
    return arr;
}

var b = false;
if (b || 100 in 1=.10)
    var a = 2; # Test redefinition checks.
else if (!(s == "String") && 2 + 2 >= 3)
    print("hi");
else {
    var a = 1;
    print(a);
}

var q = 1;
while (q % 5 != 0;)
    q += 3;
print(q);

func getNumber(arg: Number): Number {
    return arg + 53942.233 ** 0.1;
}
```
