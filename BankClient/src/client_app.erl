-module(client_app).

-behaviour(application).
-include("gen-erl/bank_schema_types.hrl").

%% API
-export([
  client_interface/0]
).

%% Application callbacks
-export([
  start/2,
  stop/1
]).

%%%===================================================================
%%% API
%%%===================================================================

client_interface() ->
  case io:fread("Select you action
   1: Register Client
   2: Get Account Balance
   3: Get Loan Cost (Premium Only)
   > ", "~d") of
    {ok, [1]} -> register_client();
    {ok, [2]} -> get_balance();
    {ok, [3]} -> get_loan();
    {_, _} -> io:format("Invalid Input~n", [])
  end,
  client_interface().

register_client() ->
  case io:fread("Enter space separated Name, Surname, Pesel, Income, Balance, Currency\n", "~s ~s ~d ~d ~d ~a") of
    {ok, [Name, Surname, Uid, Income, Balance, Currency]} ->
      io:format("~s~n", [create_account(Name, Surname, Uid, Income, Balance, Currency)]);
    {_, Reason} -> io:format("Bad Input (~p)~n", [Reason])
  end.

create_account(Forename, Surname, Uid, Income, Balance, Currency) ->
  CurrencyCode = proplists:get_value(Currency, bank_schema_types:enum_info('Currency')),
  Person = #'PersonalData'{
    name = Forename,
    surname = Surname,
    id = #'ID'{id = Uid},
    income = #'Money'{amount = Income, currency = CurrencyCode}
  },
  Init = #'Money'{amount = Balance, currency = CurrencyCode},
  gen_server:call(client_behaviour, {create_account, [Person, Init]}).

get_balance() ->
  case io:fread("Enter space separated ID, Passwd, Account Type [ S | P ]\n", "~d ~s ~a") of
    {ok, [Id, Guid, Type]} when (Type == 'P') or (Type == 'S') ->
      io:format("~s~n", [get_balance(Id, Guid, Type)]);
    {_, Reason} -> io:format("Bad Input (~p)~n", [Reason])
  end.

get_balance(Id, Passwd, 'P') ->
  Login = #'Login'{id = #'ID'{id = Id}, passwd = #'Password'{password = md5_hex(Passwd)}},
  gen_server:call(client_behaviour, {get_balance_premium, [Login]});

get_balance(Id, Passwd, 'S') ->
  Login = #'Login'{id = #'ID'{id = Id}, passwd = #'Password'{password = md5_hex(Passwd)}},
  gen_server:call(client_behaviour, {get_balance_standard, [Login]}).

get_loan() ->
  case io:fread("Enter space separated ID, Passwd, Amount, Currency, Time\n", "~d ~s ~d ~a ~d") of
    {ok, [Id, Passwd, Amount, Currency, Period]} ->
      io:format("~s~n", [get_loan(Id, Passwd, Amount, Currency, Period)]);
    {_, Reason} -> io:format("Bad Input (~p)~n", [Reason])
  end.

get_loan(Id, Passwd, Amount, Currency, Time) ->
  CurrencyCode = proplists:get_value(Currency, bank_schema_types:enum_info('Currency')),
  Login = #'Login'{id = #'ID'{id = Id}, passwd = #'Password'{password = md5_hex(Passwd)}},
  Request = #'LoanRequest'{login = Login, money = #'Money'{amount = Amount, currency = CurrencyCode}, time = Time},
  gen_server:call(client_behaviour, {get_loan_cost, [Request]}).

%% ===================================================================
%% Application callbacks
%% ===================================================================

start(_StartType, _StartArgs) ->
  spawn(?MODULE, client_interface, []),
  client_sup:start_link().


stop(_State) ->
  ok.

%% ===================================================================
%% Support functions
%% ===================================================================


md5_hex(S) ->
  Md5_bin = erlang:md5(S),
  Md5_list = binary_to_list(Md5_bin),
  lists:flatten(list_to_hex(Md5_list)).

list_to_hex(L) ->
  lists:map(fun(X) -> int_to_hex(X) end, L).

int_to_hex(N) when N < 256 ->
  [hex(N div 16), hex(N rem 16)].

hex(N) when N < 10 ->
  $0 + N;
hex(N) when N >= 10, N < 16 ->
  $a + (N - 10).
