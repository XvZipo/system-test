pragma solidity ^0.8.27;
    error AnError(uint256, string, uint256);
    error AnotherError(uint256, string, uint256);

contract C
{
    function m() external pure
    {
        require(false, AnError(1, "two", 3));
    }

    function n() external pure
    {
        require(false, AnotherError(4, "five", 6));
    }

    uint256 counter = 0;

    error CustomError(uint256);

    function getCounter() public view returns (uint256) {
        return counter;
    }

    function g(bool condition) internal returns (bool) {
        counter++;
        return condition;
    }

    function f(bool condition) external {
        require(g(condition), CustomError(counter));
    }



    error E1(uint, uint, uint, function(uint256) external pure returns (uint256));
    uint public x;

    function e1(uint256 y) external pure returns (uint256) {
        return y;
    }

    function f1(bool condition, uint a, uint b, uint c) public {
        require(condition, E1(a, b, c, this.e1));
        x = b;
    }


    error CustomError1(string);
    function f2() external pure
    {
        string memory reason = "errorReason";
        require(false, CustomError1(reason));
    }

    function g2() external pure
    {
        string memory reason = "anotherReason";
        require(false, CustomError1(reason));
    }
}

//contract D {
//    error E1(uint, uint, uint, function(uint256) external pure returns (uint256));
//    uint public x;
//
//    function e1(uint256 y) external pure returns (uint256) {
//        return y;
//    }
//
//    function f1(bool condition, uint a, uint b, uint c) public {
//        require(condition, E(a, b, c, this.e));
//        x = b;
//    }
//}
//
//
//
//    error CustomError(string);
//
//contract E
//{
//    function f2() external pure
//    {
//        string memory reason = "errorReason";
//        require(false, CustomError(reason));
//    }
//
//    function g2() external pure
//    {
//        string memory reason = "anotherReason";
//        require(false, CustomError(reason));
//    }
//}