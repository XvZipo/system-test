//pragma solidity ^0.6.0;

contract selectorContract {
    function testSelectorNoParam() external pure returns(uint) {
        return 11;
    }

    function testSelectorWithParam(uint x) external pure returns(uint) {
        return 22;
    }
}

interface interfaceSelector {
    function getSelector() external pure returns(uint);
}

interface B is interfaceSelector {
    function testImplemention() external pure returns(uint);
}

contract implementContract is B{
    function getSelector() external override pure returns(uint) {
        return 66;
    }

    function testImplemention() external override pure returns(uint) {
        return 77;
    }

    constructor() public payable {}
}

contract basicContract{
    function testNewUse() external payable returns(uint) {
        return 345;
    }

    constructor() public payable {}
}

contract TestGasValue{
    constructor() public payable {}

    function testNewUse() external payable returns(uint) {
        return 123;
    }
    basicContract bc = new basicContract();
    function callWithGasAndValue(uint x,uint y) external returns(uint) {
        return bc.testNewUse{gas:x, value:y}();
    }

    function callThisNoGasAnd1Value() external returns(uint) {
        return this.testNewUse{gas:0, value:1}();
    }

    function testAssemblyTrue() public pure returns(uint x) {
        assembly {
            x := true
        }
    }

    function testAssemblyFalse() public pure returns(uint x) {
        assembly {
            x := false
        }
    }

    function testCreate2() public returns(address) {
        basicContract c = new basicContract{salt: bytes32(bytes1(0x01)), value: 1 trx}();
        return address(c);
    }


    function getContractSelectorNoParam() public pure returns(bytes4) {
        return selectorContract.testSelectorNoParam.selector;
    }

    function getContractSelectorWithParam() public pure returns(bytes4) {
        return selectorContract.testSelectorWithParam.selector;
    }

    function getInterfaceSelectorNoParam() public pure returns(bytes4) {
        return interfaceSelector.getSelector.selector;
    }

}

