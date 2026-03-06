pragma solidity ^0.8.27;
contract C {
function transient() public pure { }
}

error CustomError(uint transient);
event e1(uint transient);
event e2(uint indexed transient);

struct S {
int transient;
}

contract D {
function f() public pure returns (uint) {
uint transient = 1;
return transient;
}

function g(int transient) public pure { }

modifier m(address transient) {
_;
}
}

contract transient {}

contract O {
    address public immutable transient;
}

contract P {
    function (uint transient) external y;
}

contract Q {
    int constant public transient = 0;
}



//Error: Expected ',' but got identifier
//contract test {
//    error e1(string transient a);
//}

//Error: Expected ',' but got identifier
//contract test {
//    event e1(string transient a);
//}

//Error: Data location must be "memory" or "calldata" for parameter in function, but none was given.
//contract C {
//    function (uint transient) external y;
//    function (uint[] transient) external z;
//}

//Error: Expected ',' but got identifier
//contract C {
//    function (uint transient x) external transient y;
//}

//Error: Transient storage variables are not supported.
//contract C {
//    function (uint transient) external transient y;
//}

//Error: Expected ',' but got identifier
//contract C {
//    function (uint transient x) external transient y;
//}

//Error: Transient cannot be used as data location for constant or immutable variables.
//contract C {
//uint public immutable transient x;
//}

//Error: Data location must be "storage", "memory" or "calldata" for parameter in function, but none was given.
//contract A {
//    modifier mod2(uint[] transient) { _; }
//}

//Error: Expected ';' but got identifier
//struct S {
//int transient x;
//}

//Error: Transient storage variables are not supported.
//contract C {
//address payable transient a;
//}

//Error: Expected identifier but got 'payable'
//contract C {
//address transient payable a;
//}

//v0.8.27 Error: Transient storage variables are not supported.
//contract e {
//uint transient transient;
//}

