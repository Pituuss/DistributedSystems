defmodule RabbitHospitalTest do
  use ExUnit.Case
  doctest RabbitHospital

  test "greets the world" do
    assert RabbitHospital.hello() == :world
  end
end
